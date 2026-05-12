# PR#222: Maintenance Upgrade to PostgreSQL 17.7 Trixie

## The Problem

Even though I had tested the application with PostgreSQL `17.7-trixie` a while ago,
the project still used PostgreSQL `15.5-bookworm` in the local Docker setup, in Testcontainers-based test
and in documentation.

After switching the tests to PostgreSQL `17.7-trixie`, the migration tests which generate reference SQL dumps failed on developer machines with an older host `pg_dump`.
PostgreSQL requires `pg_dump` to be at least as new as the database server.
Therefore, a host-side `pg_dump` 16 could not dump a PostgreSQL 17 Testcontainer database.

The failing error looked like this:

```text
pg_dump: error: aborting because of server version mismatch
pg_dump: detail: server version: 17.7 (Debian 17.7-3.pgdg13+1); pg_dump version: 16.13 (Ubuntu 16.13-0ubuntu0.24.04.1)
```

## The Solution

The PostgreSQL Docker image references were upgraded from `15.5-bookworm` to `17.7-trixie`.
This includes the custom PostgreSQL image, the Docker Compose setup, developer documentation, and the Testcontainers JDBC URLs used by integration and migration tests.

The SQL dump helper used by the migration tests was changed to run `pg_dump` inside the PostgreSQL Testcontainer.
This ensures that `pg_dump` always has the same major version as the PostgreSQL server used by the test.

## Additional Changes

The RBAC performance analysis documentation was updated to use the new PostgreSQL image tag in its examples.

## Verification

The Liquibase compatibility test was run successfully with the required PostgreSQL role environment variables:

```sh
env HSADMINNG_POSTGRES_ADMIN_USERNAME=admin \
    HSADMINNG_POSTGRES_RESTRICTED_USERNAME=restricted \
    ./gradlew test --tests net.hostsharing.hsadminng.hs.migration.LiquibaseCompatibilityIntegrationTest
```
