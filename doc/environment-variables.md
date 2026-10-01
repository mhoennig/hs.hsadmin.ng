# Environment Variables

This document lists the environment variables used in this repository,
grouped by what consumes them, with their purpose, where they are usually loaded from,
and their defaults (column _Default/Example_: a stated default also serves as an example;
where there is no default, or the effective default comes from a level below the
environment variables, an example value is given instead, prefixed with `example:`).

## Environment Files

Environment variables are usually not exported manually but loaded from these files:

| File                 | Tracked | Purpose                                                                                                                                                                                                                      |
|----------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `.tc-environment`    | yes     | canonical environment for Testcontainers-based test runs; sourced by `gw-test`/`gw-check` from `.aliases` and manually via `. .tc-environment` before Gradle test runs                                                       |
| `.environment`       | no      | current personal environment; auto-created as a copy of `.tc-environment` by `source .aliases` if missing, then sourced by `.aliases`; the place for personal overrides and secrets |
| `.environment-*`     | no      | personal variants for specific setups (e.g. `-dev-local`, `-fake-jwt-local`, `-prod-local`, `-int-remote`) to be copied onto `.environment`                                                                                  |
| `.unset-environment` | yes     | unsets all known `HSADMINNG_*` variables; sourced at the top of environment files to start from a clean slate                                                                                                                |

With `HSADMINNG_SKIP_ENVIRONMENT_FILE=true`, `source .aliases` skips sourcing the personal `.environment`;
`tools/smoke-test` exports it to protect its fully explicitly exported environment,
also in the child-process tools it exercises.


## Backend (Spring Boot Application)

Consumed via property placeholders in `src/main/resources/application.yml`,
via Spring Boot relaxed binding of matching properties,
and as Liquibase changelog parameters (see `db/changelog/0-base/009-check-environment.sql`).

The datasource credentials hardcoded in `application.yml` (`postgres`/`password`) only apply to
local deployments; the deployed stages (dev, int) override them in the `EnvironmentFile` via the
standard Spring-Boot variables `SPRING_DATASOURCE_*`, which take precedence (relaxed binding:
OS environment variables rank above the values from `application.yml`).

| Variable                                                | Purpose                                                                                                                                                                                                                          | Default/Example                                                                                                      |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `HSADMINNG_POSTGRES_JDBC_URL`                           | JDBC-URL of the PostgreSQL database (`spring.datasource.url`); shadowed by `SPRING_DATASOURCE_URL` where that is set (deployed stages)                                                                                           | none for `bootRun` (tests use Testcontainers); example: `jdbc:postgresql://localhost:5432/postgres`                  |
| `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`         | standard Spring-Boot overrides of the datasource connection; set in the `EnvironmentFile` of the deployed stages instead of the hardcoded local-dev credentials                                                                  | `HSADMINNG_POSTGRES_JDBC_URL`/`postgres`/`password` (`application.yml`)                                              |
| `SERVER_PORT`/`SERVER_ADDRESS`/`MANAGEMENT_SERVER_PORT` | standard Spring-Boot overrides for the HTTP and management listeners; set in the `EnvironmentFile` of the deployed stages                                                                                                        | `8080`/all interfaces (example: `127.0.0.1`)/`8081` (`application.yml`)                                              |
| `HSADMINNG_POSTGRES_ADMIN_USERNAME`                     | Liquibase changelog parameter: name of the admin database role                                                                                                                                                                   | none (checked by `009-check-environment.sql`); example: `postgres`                                                   |
| `HSADMINNG_POSTGRES_RESTRICTED_USERNAME`                | TODO.refa: remove, afaik, it's not effectively used                                                                                                                                                                              | none (checked by `009-check-environment.sql`); example: `restricted`                                                 |
| `HSADMINNG_POSTGRES_ADMIN_PASSWORD`                     | database password, only consumed by integration tests (TODO.impl: should we always use SPRING_DATASOURCE_*-properties?); `application.yml` itself has a hardcoded dev password                                                   | `password` (tests)                                                                                                   |
| `HSADMINNG_SUPERUSER`                                   | RBAC subject used as acting superuser in data-setup SQL (`9800-cleanup.sql`) and as `hsadminng.superuser` in migration/scenario tests. TODO.refa: Do we really need this, or maybe have a special subject just for this purpose? | `hsh-import_superuser` / `hsh-alex_superuser` (tests)                                                                |
| `HSADMINNG_JWT_ISSUER`                                  | OIDC issuer URI for real JWT authentication (`spring...jwt.issuer-uri`); ignored with the `fake-jwt` profile                                                                                                                     | empty; example: `https://login.ng.hostsharing.net/realms/hs`                                                         |
| `HSADMINNG_JWT_HMAC_SECRET`                             | alternative symmetric JWT verification via HMAC/HS512 (`spring...jwt.hmac-secret`); takes precedence over the issuer-based decoder                                                                                               | empty; example: a random string of at least 64 chars (HS512 key)                                                     |
| `HSADMINNG_FAKE_JWT_CLIENT_HOSTS`                       | hosts, to which the fake OIDC provider may redirect to (`hsadminng.fake-jwt.client-hosts`, profile `fake-jwt`); an override replaces the default list, so re-list loopback if local frontends still use this backend             | `localhost, 127.0.0.1, [::1]` (`application.yml`); example: `frontend.dev.hostsharing.net,localhost,127.0.0.1,[::1]` |
| `ALLOWED_ORIGINS`                                       | restricts the CORS origins (comma-separated); `application.yml` maps it to `hsadminng.cors.allowed-origins`, read by `BaseWebSecurityConfig`                                                                                     | `*` (allows all origins); example: `http://127.0.0.1:8082`                                                           |
| `HSADMINNG_PROVISIONING_API_KEY_SHA256`                 | SHA-256 hash (hex) of the provisioning API-key, provisions the `hsadminng.provisioning.key` subject at startup, see [Bootstrapping the provisioning API-key](../README.md#bootstrapping-the-provisioning-api-key)                | empty (bootstrap disabled)                                                                                           |

## HTTP / LOGIN / APIKEY / LOGOUT Shell Functions

Consumed by the bash functions from `.aliases` and the scripts `tools/http` and `tools/jwt-login`,
see the HOWTO chapters about the `HTTP` function and JWT-/API-key-authentication in the [README](../README.md).

| Variable                          | Purpose                                                                                                                                                                                                     | Default/Example                              |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `HSADMINNG_API_BASE_URL`          | base URL prefixed by `HTTP` to relative `/api/...` paths                                                                                                                                                    | none (required for relative paths); example: `http://localhost:8080` |
| `HSADMINNG_JWT_BEARER`            | JWT access token, implicitly sent by `HTTP` as `Authorization: Bearer ...`; exported by `LOGIN` (via `tools/jwt-login`). TODO.refa: should this be renamed to `HSADMINNG_CURRENT_JWT_BEARER`?               | none; example: `eyJhbGciOiJSUzI1NiIs...` (a JWT) |
| `HSADMINNG_API_KEY`               | clear-text API-key, implicitly sent by `HTTP` as `Hostsharing-Api-Key` header if `HSADMINNG_JWT_BEARER` is not set; exported by `APIKEY`  TODO.refa: should this be renamed to `HSADMINNG_CURRENT_API_KEY`? | none                                         |
| `HSADMINNG_API_KEY_<NAME>`        | API-keys remembered by name for the `APIKEY` function; can be predefined in the git-ignored `.environment`                                                                                                  | none (`APIKEY` asks once)                    |
| `HSADMINNG_API_KEY_NAME`          | bookkeeping of the `APIKEY` function: the most recently used API-key name, re-used by a plain `APIKEY` call. TODO.refa: should this be renamed to `HSADMINNG_CURRENT_API_KEY_NAME`?                         | none                                         |
| `HSADMINNG_JWT_USERNAME`          | username for `LOGIN`/`tools/jwt-login`. TODO.refa: should this be renamed to `HSADMINNG_CURRENT_JWT_USERNAME`?                                                                                              | prompted if unset; example: `hsh-alex_superuser` |
| `HSADMINNG_JWT_PASSWORD`          | password for `LOGIN`/`tools/jwt-login`. TODO.refa: should this be renamed to `HSADMINNG_CURRENT_JWT_PASSWORD`?                                                                                              | prompted if unset                            |
| `HSADMINNG_JWT_TOKEN_URL`         | if set, `tools/jwt-login` POSTs a direct OAuth2 password grant to this token endpoint instead of the Keycloak login-form flow; `.tc-environment` points it to the local fake-jwt endpoint                   | none (login-form flow); example: `http://localhost:8080/fake-jwt/token` |
| `HSADMINNG_JWT_CLIENT_ID`         | OAuth2 client-id for the password grant against `HSADMINNG_JWT_TOKEN_URL`                                                                                                                                   | none; example: `hsscript.ng` (as in `.tc-environment`) |
| `HSADMINNG_JWT_CLIENT_SECRET`     | OAuth2 client-secret for the password grant, if the client requires one                                                                                                                                     | none                                         |
| `HSADMINNG_KEYCLOAK_ISSUER`       | Keycloak realm URL for the login-form flow of `tools/jwt-login`                                                                                                                                             | `https://login.ng.hostsharing.net/realms/hs` |
| `HSADMINNG_KEYCLOAK_CLIENT_ID`    | OAuth2 client-id for the login-form flow                                                                                                                                                                    | `hs-admin-web`                               |
| `HSADMINNG_KEYCLOAK_REDIRECT_URI` | redirect URI for the login-form flow                                                                                                                                                                        | `https://testui.ng.hostsharing.net/`         |

## Monitoring (`tools/monitoring-check`)

This self-contained monitoring script (intended to be copied to a monitoring host) reuses
`HSADMINNG_API_BASE_URL` and the `HSADMINNG_KEYCLOAK_*` variables from the previous section,
but with a monitoring-specific default (base URL `https://backend.ng.hostsharing.net`).
The credentials of the login check come from dedicated `HSADMINNG_MONITORING_LOGIN_*` variables, deliberately separate
from the `HSADMINNG_JWT_*` variables of the interactive `LOGIN` tooling, so that both can
coexist in a shared environment file.

`tools/remote monitoring deploy` installs a systemd user timer on the backend server which runs
this check every 5 minutes (default); the timer reads its variables from `~/.environment` on the server
(systemd `EnvironmentFile` format: plain `VAR=value` lines, no `export`), which must contain
at least `HSADMINNG_MONITORING_LOGIN_PASSWORD` and `HSADMINNG_MATRIX_WEBHOOK_URL`.

| Variable                                               | Purpose                                                                                                                                                                                                                                                                                                                | Default/Example                                                                                        |
|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `HSADMINNG_MONITORING_LOGIN_USERNAME`                  | Keycloak username of the unprivileged monitoring user                                                                                                                                                                                                                                                                  | `hsh-login_monitor`                                                                                    |
| `HSADMINNG_MONITORING_LOGIN_PASSWORD`                  | the monitoring user's Keycloak password                                                                                                                                                                                                                                                                                | none (required)                                                                                        |
| `HSADMINNG_MATRIX_WEBHOOK_URL`                         | Matrix webhook URL (e.g. a matrix-hookshot generic webhook) to which non-OK results are posted when the script is called with a bare `--matrix-webhook` option; severity-coloured (⚠️ slow, 🚨 failed, 🆘 critical with `@room`); keeps the secret URL out of the process list                                           | none (no alerting without `--matrix-webhook`); example: `https://hookshot.example.com/webhook/<token>` |
| `HSADMINNG_MONITORING_ALARM_REPEAT`                    | backoff for repeated Matrix alerts while a problem persists: comma-separated intervals in hours between alerts, the last one repeating indefinitely; the first occurrence is always alerted immediately, and a `RESOLVED` message is posted once it clears; each alarm backs off on its own, so none can delay another | `1,2,4,8,16,24`                                                                                        |
| `HSADMINNG_MONITORING_SLOW_WARNING_SECONDS`            | a successful but slower-than-this (seconds) complete check raises a ⚠️ latency warning (baseline is ~0.5 s); throttled like alerts via a sibling state file                                                                                                                                                            | `2`                                                                                                    |
| `HSADMINNG_MONITORING_SYNC_KEY_NAME`                   | API-key subject-name whose Keycloak-sync runs the check verifies via `GET /api/rbac/subjects/last-sync`; a 🚨 sync-freshness alarm fires when no fresh report is found                                                                                                                                                  | `hsadminng.keycloak.sync`                                                                              |
| `HSADMINNG_MONITORING_SYNC_MAX_AGE_SECONDS`            | maximum tolerated age (seconds) of the last Keycloak-sync report before the sync check fails; `0` disables the sync check entirely; a non-numeric value is ignored with a warning and falls back to the default                                                                                                        | `300` (`0` disables)                                                                                   |
| `HSADMINNG_MONITORING_BANK_DIRECTORY_MAX_AGE_DAYS`     | maximum tolerated age (days) of the imported bank directory before the bank-directory check fails; `0` disables both bank-directory checks entirely, e.g. on stages without the import; a non-numeric value is ignored with a warning and falls back to the default                                                    | `100` (`0` disables)                                                                                   |
| `HSADMINNG_MONITORING_BANK_DIRECTORY_MAX_RUN_AGE_DAYS` | maximum tolerated age (days) of the last successful bank-directory import run, which the backend attempts weekly; a non-numeric value is ignored with a warning and falls back to the default                                                                                                                          | `8`                                                                                                    |
| `HSADMINNG_MONITORING_STATE_FILE`                      | file carrying the active-alarm state (first failure, alert count) between runs, enabling the alert backoff and the `RESOLVED` message; the gained-privileges, latency, sync and bank-directory alarms use the sibling files `*.critical.state`, `*.slow.state`, `*.sync.state` and `*.bank-directory.state`            | `~/.local/state/monitoring-check.state`                                                                |
| `HSADMINNG_MONITORING_INTERVAL_MINUTES`                | minutes between the timer runs (1–59); read from `~/.environment` on the server by `remote monitoring deploy` when generating the systemd timer, so re-run that command after changing it                                                                                                                              | `5`                                                                                                    |

## Remote Backend Server (`tools/remote`)

`tools/remote` targets a test stage to try out unmerged changes; regular releases go via CI/CD.

| Variable                                | Purpose                                                                                                                             | Default/Example        |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| `HSADMINNG_DEPLOY_HOST`                 | bare host of the deploy stage (e.g. `hsh03`) for all `remote` commands, which log in as `<host>-backend` or, for uploads, `<host>-ansible`; no default, to avoid using unintended stage | must be set explicitly |
| `HSADMINNG_PROVISIONING_API_KEY_SHA256` | provisioned server-side by `remote backend provision-api-key` into the `EnvironmentFile` of the backend service (see backend table) | empty                  |

## Tests and Build

Consumed by JUnit tests (`src/test/...`), the Gradle build, and Testcontainers.

| Variable                                  | Purpose                                                                                                                                                                                                                                 | Default/Example                                                            |
|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `HSADMINNG_POSTGRES_JDBC_URL`             | if set, integration tests run against that database instead of an ephemeral Testcontainers instance, see [How to Use a Persistent Database for Integration Tests?](../README.md#how-to-use-a-persistent-database-for-integration-tests) | `jdbc:tc:postgresql:17.7-trixie:///...` (Testcontainers)                   |
| `HSADMINNG_MIGRATION_DATA_PATH`           | resource directory with the legacy CSV data for the import-/migration-tests (`CsvDataImport`)                                                                                                                                           | `migration` (anonymized test data)                                         |
| `HSADMINNG_OFFICE_DATA_SQL_FILE`          | SQL dump with office data used by `ImportHostingAssets`; empty value means the default resource                                                                                                                                         | `/db/released-prod-schema-with-import-test-data.sql`                       |
| `HSADMINNG_SCENARIO_HTTP_TIMEOUT_SECONDS` | HTTP timeout for scenario-test requests (`UseCase`)                                                                                                                                                                                     | `20`                                                                       |
| `HSADMINNG_TEST_MAX_PARALLEL_FORKS`       | max parallel test JVMs for the tag-scoped Gradle test tasks (`HsadminTestTasksPlugin`)                                                                                                                                                  | `3`                                                                        |
| `DOCKER_HOST`                             | Docker-/Podman-socket used by Testcontainers, see the Podman HOWTO chapters in the [README](../README.md)                                                                                                                               | Docker default socket; example: `unix:///run/user/1000/podman/podman.sock` |
| `TESTCONTAINERS_RYUK_DISABLED`            | disables the Testcontainers cleanup container, needed for Podman, see the Podman HOWTO chapters in the [README](../README.md)                                                                                                           | `false`                                                                    |
| `TESTCONTAINERS_HOST_OVERRIDE`            | address under which Testcontainers and `tools/smoke-test` reach published container ports; needed where they do not listen on `127.0.0.1`, e.g. a rootless Docker with slirp4netns offers them on `10.0.2.100`                          | none; example: `10.0.2.100`                                                |

## Smoke-Test (`tools/smoke-test`)

`tools/smoke-test` boots the application against a throw-away Docker-PostgreSQL and exercises the shell tooling;
it exports its complete environment itself, so these variables only adjust ports and addresses.

| Variable                               | Purpose                                                                                                                                                                                           | Default/Example                                         |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| `HSADMINNG_SMOKE_TEST_PG_PORT`         | host port of the throw-away database container                                                                                                                                                    | `15432`                                                 |
| `HSADMINNG_SMOKE_TEST_PG_HOST`         | address the application reaches that port on; the bind address `127.0.0.1` of the container refers to the Docker daemon's network, which with slirp4netns offers the port on `10.0.2.100` instead | `TESTCONTAINERS_HOST_OVERRIDE` if set, else `127.0.0.1` |
| `HSADMINNG_SMOKE_TEST_APP_PORT`        | application port, the management port is this plus one                                                                                                                                            | `8090`                                                  |
| `HSADMINNG_SMOKE_TEST_STARTUP_TIMEOUT` | seconds to wait for the application to start                                                                                                                                                      | `300`                                                   |

## CI and Miscellaneous

| Variable                      | Purpose                                                                                                         | Default/Example                       |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------|---------------------------------------|
| `GIT_USERNAME`/`GIT_PASSWORD` | git credentials for the Jenkins CI configuration (`Jenkins/jenkins.yaml`), see `Jenkins/README.md`              | none                                  |
| `LANG`                        | set to `en_US.UTF-8` by the environment files for locale-independent sorting and formatting in builds and tests | system locale; example: `en_US.UTF-8` |
