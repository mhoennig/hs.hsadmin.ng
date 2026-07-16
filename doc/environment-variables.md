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


## Backend (Spring Boot Application)

Consumed via property placeholders in `src/main/resources/application.yml`
and as Liquibase changelog parameters (see `db/changelog/0-base/009-check-environment.sql`).

The datasource credentials hardcoded in `application.yml` (`postgres`/`password`) only apply to
local deployments; the deployed stages (dev, int) override them in the `EnvironmentFile` via the
standard Spring-Boot variables `SPRING_DATASOURCE_*`, which take precedence (relaxed binding:
OS environment variables rank above the values from `application.yml`).

| Variable                                                | Purpose                                                                                                                                                                                                                          | Default/Example                                                         |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `HSADMINNG_POSTGRES_JDBC_URL`                           | JDBC-URL of the PostgreSQL database (`spring.datasource.url`); shadowed by `SPRING_DATASOURCE_URL` where that is set (deployed stages)                                                                                           | none for `bootRun` (tests use Testcontainers); example: `jdbc:postgresql://localhost:5432/postgres` |
| `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`         | standard Spring-Boot overrides of the datasource connection; set in the `EnvironmentFile` of the deployed stages instead of the hardcoded local-dev credentials                                                                  | `HSADMINNG_POSTGRES_JDBC_URL`/`postgres`/`password` (`application.yml`) |
| `SERVER_PORT`/`SERVER_ADDRESS`/`MANAGEMENT_SERVER_PORT` | standard Spring-Boot overrides for the HTTP and management listeners; set in the `EnvironmentFile` of the deployed stages                                                                                                        | `8080`/all interfaces (example: `127.0.0.1`)/`8081` (`application.yml`) |
| `HSADMINNG_POSTGRES_ADMIN_USERNAME`                     | Liquibase changelog parameter: name of the admin database role                                                                                                                                                                   | none (checked by `009-check-environment.sql`); example: `postgres`      |
| `HSADMINNG_POSTGRES_RESTRICTED_USERNAME`                | TODO.refa: remove, afaik, it's not effectively used                                                                                                                                                                              | none (checked by `009-check-environment.sql`); example: `restricted`    |
| `HSADMINNG_POSTGRES_ADMIN_PASSWORD`                     | database password, only consumed by integration tests (TODO.impl: should we always use SPRING_DATASOURCE_*-properties?); `application.yml` itself has a hardcoded dev password                                                   | `password` (tests)                                                      |
| `HSADMINNG_SUPERUSER`                                   | RBAC subject used as acting superuser in data-setup SQL (`9800-cleanup.sql`) and as `hsadminng.superuser` in migration/scenario tests. TODO.refa: Do we really need this, or maybe have a special subject just for this purpose? | `hsh-import_superuser` / `hsh-alex_superuser` (tests)                   |
| `HSADMINNG_JWT_ISSUER`                                  | OIDC issuer URI for real JWT authentication (`spring...jwt.issuer-uri`); ignored with the `fake-jwt` profile                                                                                                                     | empty; example: `https://login.ng.hostsharing.net/realms/hs`            |
| `HSADMINNG_JWT_HMAC_SECRET`                             | alternative symmetric JWT verification via HMAC/HS512 (`spring...jwt.hmac-secret`); takes precedence over the issuer-based decoder                                                                                               | empty; example: a random string of at least 64 chars (HS512 key)        |
| `ALLOWED_ORIGINS`                                       | intended to restrict CORS origins (comma-separated); note: `application.yml` maps it to `hsadminng.cors.allowed-origin` (singular), but the code reads `...allowed-origins` (plural), so it currently has no effect              | `*` (in the code); example: `http://127.0.0.1:8082`                     |

## HTTP / LOGIN / LOGOUT Shell Functions

Consumed by the bash functions from `.aliases` and the scripts `tools/http` and `tools/jwt-login`,
see the HOWTO chapters about the `HTTP` function and JWT-authentication in the [README](../README.md).

| Variable                          | Purpose                                                                                                                                                                                                     | Default/Example                              |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `HSADMINNG_API_BASE_URL`          | base URL prefixed by `HTTP` to relative `/api/...` paths                                                                                                                                                    | none (required for relative paths); example: `http://localhost:8080` |
| `HSADMINNG_JWT_BEARER`            | JWT access token, implicitly sent by `HTTP` as `Authorization: Bearer ...`; exported by `LOGIN` (via `tools/jwt-login`). TODO.refa: should this be renamed to `HSADMINNG_CURRENT_JWT_BEARER`?               | none; example: `eyJhbGciOiJSUzI1NiIs...` (a JWT) |
| `HSADMINNG_JWT_USERNAME`          | username for `LOGIN`/`tools/jwt-login`. TODO.refa: should this be renamed to `HSADMINNG_CURRENT_JWT_USERNAME`?                                                                                              | prompted if unset; example: `hsh-alex_superuser` |
| `HSADMINNG_JWT_PASSWORD`          | password for `LOGIN`/`tools/jwt-login`. TODO.refa: should this be renamed to `HSADMINNG_CURRENT_JWT_PASSWORD`?                                                                                              | prompted if unset                            |
| `HSADMINNG_JWT_TOKEN_URL`         | if set, `tools/jwt-login` POSTs a direct OAuth2 password grant to this token endpoint instead of the Keycloak login-form flow; `.tc-environment` points it to the local fake-jwt endpoint                   | none (login-form flow); example: `http://localhost:8080/fake-jwt/token` |
| `HSADMINNG_JWT_CLIENT_ID`         | OAuth2 client-id for the password grant against `HSADMINNG_JWT_TOKEN_URL`                                                                                                                                   | none; example: `hsscript.ng` (as in `.tc-environment`) |
| `HSADMINNG_JWT_CLIENT_SECRET`     | OAuth2 client-secret for the password grant, if the client requires one                                                                                                                                     | none                                         |
| `HSADMINNG_KEYCLOAK_ISSUER`       | Keycloak realm URL for the login-form flow of `tools/jwt-login`                                                                                                                                             | `https://login.ng.hostsharing.net/realms/hs` |
| `HSADMINNG_KEYCLOAK_CLIENT_ID`    | OAuth2 client-id for the login-form flow                                                                                                                                                                    | `hs-admin-web`                               |
| `HSADMINNG_KEYCLOAK_REDIRECT_URI` | redirect URI for the login-form flow                                                                                                                                                                        | `https://testui.ng.hostsharing.net/`         |

## Login-Monitoring (`tools/login-monitoring-check`)

This self-contained monitoring script (intended to be copied to a monitoring host) reuses
`HSADMINNG_API_BASE_URL` and the `HSADMINNG_KEYCLOAK_*` variables from the previous section,
but with a monitoring-specific default (base URL `https://backend.ng.hostsharing.net`).
The credentials come from dedicated `HSADMINNG_LOGIN_MONITOR_*` variables, deliberately separate
from the `HSADMINNG_JWT_*` variables of the interactive `LOGIN` tooling, so that both can
coexist in a shared environment file.

`tools/remote install-monitoring` installs a systemd user timer on the backend server which runs
this check every 5 minutes (default); the timer reads its variables from `~/.environment` on the server
(systemd `EnvironmentFile` format: plain `VAR=value` lines, no `export`), which must contain
at least `HSADMINNG_LOGIN_MONITOR_PASSWORD` and `HSADMINNG_MATRIX_WEBHOOK_URL`.

| Variable                           | Purpose                                                                                                                                                                                                       | Default/Example                                |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| `HSADMINNG_LOGIN_MONITOR_USERNAME` | Keycloak username of the unprivileged monitoring user                                                                                                                                                          | `hsh-login_monitor`                            |
| `HSADMINNG_LOGIN_MONITOR_PASSWORD` | the monitoring user's Keycloak password                                                                                                                                                                        | none (required)                                |
| `HSADMINNG_MATRIX_WEBHOOK_URL`     | Matrix webhook URL (e.g. a matrix-hookshot generic webhook) to which `FAILED` results are posted when the script is called with a bare `--matrix-webhook` option; keeps the secret URL out of the process list | none (no alerting without `--matrix-webhook`); example: `https://hookshot.example.com/webhook/<token>` |
| `HSADMINNG_LOGIN_MONITOR_ALARM_REPEAT` | backoff for repeated Matrix alerts while a failure persists: comma-separated intervals in hours between alerts, the last one repeating indefinitely; the first failure is always alerted immediately, and a `RESOLVED` message is posted once the check succeeds again | `1,2,4,8,16,24`                          |
| `HSADMINNG_LOGIN_MONITOR_STATE_FILE` | file carrying the active-alarm state (first failure, alert count) between runs, enabling the alert backoff and the `RESOLVED` message                                                                        | `~/.local/state/login-monitoring-check.state` |
| `HSADMINNG_LOGIN_MONITOR_INTERVAL_MINUTES` | minutes between the timer runs (1–59); read from `~/.environment` on the server by `remote install-monitoring` when generating the systemd timer, so re-run that command after changing it            | `5`                                            |

## Remote Backend Server (`tools/remote`)

`tools/remote` targets a test stage: it deploys the local working state to try out changes
which are not yet merged into the target branch (master/prod); regular releases are rolled
out via the CI/CD pipeline instead.

| Variable                             | Purpose                                                                                                                 | Default/Example                       |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| `HSADMINNG_DEPLOY_HOST`              | SSH `user@host` of the backend server used by `remote deploy`, `remote log` and `remote ssh`                               | `hsh11-backend@hsh11.hostsharing.net` |

## Tests and Build

Consumed by JUnit tests (`src/test/...`), the Gradle build, and Testcontainers.

| Variable                                  | Purpose                                                                                                                                   | Default/Example                                      |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| `HSADMINNG_POSTGRES_JDBC_URL`             | if set, integration tests run against that database instead of an ephemeral Testcontainers instance, see [How to Use a Persistent Database for Integration Tests?](../README.md#how-to-use-a-persistent-database-for-integration-tests) | `jdbc:tc:postgresql:17.7-trixie:///...` (Testcontainers) |
| `HSADMINNG_MIGRATION_DATA_PATH`           | resource directory with the legacy CSV data for the import-/migration-tests (`CsvDataImport`)                                                 | `migration` (anonymized test data)                   |
| `HSADMINNG_OFFICE_DATA_SQL_FILE`          | SQL dump with office data used by `ImportHostingAssets`; empty value means the default resource                                               | `/db/released-prod-schema-with-import-test-data.sql` |
| `HSADMINNG_SCENARIO_HTTP_TIMEOUT_SECONDS` | HTTP timeout for scenario-test requests (`UseCase`)                                                                                           | `20`                                                 |
| `HSADMINNG_TEST_MAX_PARALLEL_FORKS`       | max parallel test JVMs for the tag-scoped Gradle test tasks (`HsadminTestTasksPlugin`)                                                        | `3`                                                  |
| `DOCKER_HOST`                             | Docker-/Podman-socket used by Testcontainers, see the Podman HOWTO chapters in the [README](../README.md)                                     | Docker default socket; example: `unix:///run/user/1000/podman/podman.sock` |
| `TESTCONTAINERS_RYUK_DISABLED`            | disables the Testcontainers cleanup container, needed for Podman, see the Podman HOWTO chapters in the [README](../README.md)                 | `false`                                              |

## CI and Miscellaneous

| Variable                    | Purpose                                                                                                             | Default/Example |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------|---------------|
| `GIT_USERNAME`/`GIT_PASSWORD` | git credentials for the Jenkins CI configuration (`Jenkins/jenkins.yaml`), see `Jenkins/README.md`                      | none          |
| `LANG`                      | set to `en_US.UTF-8` by the environment files for locale-independent sorting and formatting in builds and tests          | system locale; example: `en_US.UTF-8` |
