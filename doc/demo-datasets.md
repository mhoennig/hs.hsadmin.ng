# Generating and Working With Demo Datasets

`tools/demo-data` covers the whole workflow: `generate`, `dump`, `upload`, `restore`, `run`, `verify`, `list` and `drop`.
Use `tools/demo-data --help` to learn about the verbs, options and environment variables.
Use `--dry-run` to see what commands a call would run, without running them.
This document explains the background that the help text does not cover.

The concept and reasons are in [RFC#0005](rfc/2026-08-06-RFC%230005-generated-demo-and-test-data-for-hs-office.md).

## One Database per Dataset

The generator uses a persistent database, not a temporary test container that disappears.
It writes one dataset into one database.
The application selects a dataset when it starts using its JDBC URL.
Each PostgreSQL version has its own container.
All datasets stay in the container even when it stops.
Only remove the container to delete the datasets.

All datasets use the same login `hsh-demo_admin` and the same partner numbers from `P-90000`.
They can exist as separate databases.
However, you cannot restore them into one database together.

If the application reports `FATAL: password authentication failed` but the container runs,
another service probably uses the container's host port instead.

## What Has to Stop, and What Does Not

The container does not need to stop.
It keeps its data even when you stop it.
The generator starts its own application on a random port.
This lets you create a new dataset while the application uses another one.
Stop the application only to switch it to another dataset,
or to drop a database it is connected to.
`drop database` fails if other users access it: `database "..." is being accessed by other users`.

## Generating

A typical developer laptop generates about 35 customers per minute.
100 customers took 3 minutes, 500 customers took 15 minutes.
The rate slows down as the dataset grows.
This happens because `base.tx_context` and the grant graph grow too.

`demo-data generate` sends its arguments to the generator using two parameters:
`hsadminng.demodata.customerCount` and `hsadminng.demodata.personsCsv`.
The third parameter is `hsadminng.demodata.seed`, it stays at a fixed value.
This way, the same call always generates the same data.

Stages that use Keycloak for login get the fixed-persons CSV from `tools/get-keycloak-users`.
RFC#0005 explains which persons it names and why.
Each dataset has its own person UUIDs and its own CSV file for `tools/create-accounts-from-csv`.
Do not use the CSV from another dataset—it refers to persons that do not exist.
Always use the CSV that matches the dataset.

## What the Dump Excludes, and Why

The dump uses `--format=custom`.
This makes it about five times smaller than a plain dump with `--column-inserts`.
It also restores faster because it uses COPY instead of single INSERT statements.

**API-key data must never leave the container.**
The generation run creates an API-key for `ScenarioTest`.
Its clear-text is a constant in this repository.
This key has the global ADMIN role.
It never expires.
It has no scope limits.
Anyone who reads the repository could use it on the restored database.
This exclusion is safe.
No changeset creates API-keys.
The subject stays without a credential.
When the application starts again, `ProvisioningApiKeyBootstrap` finds that the subject has no key.
It creates a new one using the target environment's `HSADMINNG_PROVISIONING_API_KEY_SHA256` (see `ProvisioningApiKeyBootstrapAcceptanceTest`).

**The audit journal is excluded too.**
It records the generation run itself.
A demo system does not need this.
The audit journal is also the largest table by far.
For 500 customers, the dump size drops from 22 MB to 13 MB.
`base.tx_context` stays because other tables reference it.
Only its child table `base.tx_journal` can be excluded without breaking references.

A backup of a production database is different.
There, API-keys must stay in the dump.
You can exclude them at restore time instead.
Use `pg_restore --list` to get the table of contents.
Remove the `TABLE DATA rbac api_key` lines.
Then restore with `pg_restore --use-list`.
