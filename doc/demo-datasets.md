# Generating and Working With Demo Datasets

The generator runs as a scenario test against a persistent database,
because the Testcontainers database of a test run disappears with it.
It writes one dataset into one database.
To keep more than one dataset, give each its own database in the same container.
The application selects a dataset at start through its JDBC URL.

The concept and the reasons behind it are in
[RFC#0005](rfc/2026-08-06-RFC%230005-generated-demo-and-test-data-for-hs-office.md).

## The Database Container

All datasets live in one container of their own:

```sh
docker run --name hsadmin-demo -e POSTGRES_PASSWORD=password -p 55433:5432 -d postgres:17.7-trixie
```

Liquibase creates the schema at the first application start, thus a fresh database needs no preparation.

**Do not publish the container on 5432.** A service which listens only on `127.0.0.1:5432` does not stop
the container from starting, but it receives the connections instead of the container.
The application then reports `FATAL: password authentication failed`, which points in the wrong direction.
Check a port with `ss -ltn | grep 55433` before you take it.

The datasets survive a stopped container; only removing it loses them.
`docker start hsadmin-demo` brings them back.

## What Has to Stop, and What Does Not

The container never has to stop, and it keeps its data if it does.
The generator brings its own application on a random port, thus generating into a *new* database
while the application serves another one is safe.
Stop the application only to switch it to another dataset, or to drop a database it is connected to:
`drop database` fails with `database "..." is being accessed by other users`.

## Generate a Dataset

Each dataset needs an empty database of its own.
The generator asserts that and stops with `[partners from other test-data] expected: 0 but was: ...`.

```sh
docker exec hsadmin-demo psql -U postgres \
    -c 'drop database if exists demodata250' \
    -c 'create database demodata250'
```

Give the fixed-person CSV a name of its own as well.
The generator derives the name of its output file from the input file,
thus a shared input would overwrite the person UUIDs of the dataset generated before:

```sh
cp .private/keycloak-users.csv .private/keycloak-users-250.csv
```

Then generate.
The `export` lines have to follow `. .tc-environment`, which would otherwise overwrite them:

```sh
. .tc-environment
export HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://localhost:55433/demodata250
export HSADMINNG_POSTGRES_ADMIN_USERNAME=postgres
export HSADMINNG_POSTGRES_ADMIN_PASSWORD=password
export HSADMINNG_POSTGRES_RESTRICTED_USERNAME=restricted
export HSADMINNG_SUPERUSER=hsh-demo_admin

./gradlew scenarioTest --tests '*DemoDataScenarioTest' \
    -Dhsadminng.demodata.customerCount=250 \
    -Dhsadminng.demodata.personsCsv=.private/keycloak-users-250.csv
```

| system property                    | effect                                                                  | default  |
|------------------------------------|-------------------------------------------------------------------------|----------|
| `hsadminng.demodata.customerCount` | the number of generated customers                                       | 5        |
| `hsadminng.demodata.seed`          | the seed of the random generator; the same seed gives the same data     | 20260805 |
| `hsadminng.demodata.personsCsv`    | a CSV with the fixed persons, as exported by `tools/get-keycloak-users` | none     |

Omit `-Dhsadminng.demodata.personsCsv` to generate customers without the fixed person set;
which persons that file names and why it exists is in RFC#0005.
The generator writes it back with the `personUuid` column filled in and named after the input,
thus `keycloak-users-250.csv` yields `keycloak-users-250-with-person-uuids.csv`.

Expect roughly 35 customers per minute: 100 customers took 3 minutes, 500 customers took 15 minutes.
The rate falls as the dataset grows, because `base.tx_context` and the grant graph grow with it.

## Switch the Application Between Datasets

Stop the running application, then start it against the other database.
Only the database name in the first line differs between datasets:

```sh
DATASET=demodata500

HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://localhost:55433/$DATASET \
HSADMINNG_POSTGRES_ADMIN_USERNAME=postgres \
HSADMINNG_POSTGRES_ADMIN_PASSWORD=password \
HSADMINNG_POSTGRES_RESTRICTED_USERNAME=restricted \
HSADMINNG_SUPERUSER=hsh-demo_admin \
./gradlew bootRun --args=--spring.profiles.active=fake-jwt,without-test-data
```

**The Liquibase context `without-test-data` is not optional.**
Without it the application applies the test-data changesets at start
and puts 5 test partners and 53 test subjects into the dataset, permanently.

Only one application can hold port 8080, thus stop the running one before you start the next.

`hsh-demo_admin` is the only login in every generated dataset, and with the profile `fake-jwt`
any password is accepted:

```sh
export HSADMINNG_API_BASE_URL=http://localhost:8080
export HSADMINNG_JWT_TOKEN_URL=http://localhost:8080/fake-jwt/token
LOGIN hsh-demo_admin
HTTP GET /api/hs/office/partners
```

## Write a Dump of a Dataset

A dump hands a dataset to somebody else, for example to the operations team.
Name the database in the last argument of `pg_dump`, because each dataset has a database of its own:

```sh
DATASET=demodata500

docker exec hsadmin-demo pg_dump -U postgres --format=custom --file=/tmp/$DATASET.dump \
    --exclude-table-data=rbac.api_key --exclude-table-data=rbac.api_key_scope \
    --exclude-table-data=base.tx_journal \
    $DATASET
docker cp hsadmin-demo:/tmp/$DATASET.dump .private/demo-dataset-$DATASET-$(date +%F).dump
```

Use `--format=custom`. It is about five times smaller than a plain dump with `--column-inserts`,
and it restores with COPY instead of single INSERT statements.

**Always exclude the API-key data.** The test run provisions the API-key of `ScenarioTest`,
whose clear-text is a constant in this repository. That key has the global ADMIN role, no expiry
and no scope, thus everybody who reads the repository could use it against the restored database.
The exclusion loses nothing: no changeset seeds an API-key, and the subject stays without a credential.
At the next application start, `ProvisioningApiKeyBootstrap` finds that subject without a stored key
and provisions the target environment's own `HSADMINNG_PROVISIONING_API_KEY_SHA256` for it
(see `ProvisioningApiKeyBootstrapAcceptanceTest`).

**The audit journal is excluded too.** It records the generation itself, which nobody needs on a demo
system, and it is the largest table by far: for 500 customers it takes the dump from 22 MB down to 13 MB.
`base.tx_context` stays, because the historicization tables of booking, hosting and accounts
reference it; only its child table `base.tx_journal` can be dropped without a dangling reference.

### Prove That the Dump Carries No Credentials

Run this check inside the container, and not on the development machine:

```sh
docker exec hsadmin-demo pg_restore --list /tmp/$DATASET.dump \
    | grep -E 'TABLE DATA (rbac api_key|base tx_journal )'
```

It has to print nothing.
A local `pg_restore` older than the server stops with `unsupported version (1.16) in file header`;
that error goes to stderr, thus `grep` finds nothing and the check looks like it passed.

### Restore a Dump

```sh
docker exec hsadmin-demo psql -U postgres -c "create database ${DATASET}_restored"
docker cp .private/demo-dataset-$DATASET-$(date +%F).dump hsadmin-demo:/tmp/restore.dump
docker exec hsadmin-demo pg_restore -U postgres --dbname=${DATASET}_restored /tmp/restore.dump
```

A backup of a production database is a different case: there the API-keys must stay in the dump,
thus exclude them at restore time instead:

```sh
pg_restore --list backup.dump | grep -v 'TABLE DATA rbac api_key' > restore.list
pg_restore --use-list=restore.list --dbname=... backup.dump
```

## Keep Track of Which File Belongs to Which Dataset

Every dataset has its own person UUIDs, thus its own CSV for `tools/create-accounts-from-csv`.
Using the file of another dataset points the tooling at persons which do not exist there,
therefore name the files after their dataset and hand a dump over together with its own CSV.

All datasets use the same login `hsh-demo_admin` and the same partner numbers from `P-90000`.
They can therefore live next to each other as separate databases,
but they cannot be restored into one database together.

To list the datasets which exist:

```sh
docker exec hsadmin-demo psql -U postgres -l
```
