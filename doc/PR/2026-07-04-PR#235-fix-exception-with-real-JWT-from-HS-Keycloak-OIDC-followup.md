# PR#235: Real JWT from HS Keycloak OIDC (follow-up to PR#220)

Follow-up branch to [PR#220](2026-03-29-PR%23220-Fix-exception-with-real-JWT-from-HS-Keycloak-OIDC.md).

See also [Story#458: Support für hsadmin-NG Start mit echtem JWT vom Keycloak](https://plan.hostsharing.net/project/admin-hsadmin/us/458) - sorry, not a public link.

## Scope (work in progress)

This branch continues the JWT/OIDC integration work from PR#220.
It's mostly about cleanup, tooling, and adaptations to the real JWTs issued by the HS Keycloak,
plus hardening of the JWT validation and some fixes discovered along the way,
with the goal of making the integration smoothly work.

- improved OIDC compatibility of the fake-JWT support, including refresh tokens
  (`FakeJwtController`, `JwtFakeBearer`);
- HMAC secret configuration via `HSADMINNG_JWT_HMAC_SECRET` through `application.yml`;
- clearer separation of fake-JWT and real-JWT configuration for tests
  (`BaseWebSecurityConfig` and related test configs);
- integration/acceptance tests for `defineContext` with an unknown subject;
- API requests authenticated with a JWT whose subject UUID has no matching `rbac.subject` row
  now return `404 Not Found` instead of `500 Internal Server Error` (`Context.subjectName`);
- Keycloak helper scripts: `tools/jwt-login`, `tools/get-keycloak-users`,
  `tools/natural-partner-person-by-name`, `tools/enrich-keycloak-users-with-existing-natural-person-uuid`;
- `GET /api/hs/accounts/accounts` (without `personUuid`) now returns all accounts for an
  effective global-admin; for other subjects still the accounts of the login-account's person;
- `POST /api/hs/accounts/accounts` now returns `409 Conflict` instead of `400 Bad Request`
  when the subject already has an account or already exists (new `ConflictException`);
- unified `HTTP` command in `tools/http` (base-URL prefix, implicit Authorization header,
  jq pretty-printing), replacing the former `HTTP`/`http` functions in `.aliases`;
  `tools/jwt-login`, when sourced, exports `HSADMINNG_JWT_BEARER` itself;
- build fix: `processSpring*` tasks track the whole `api-definition` directory as input,
  so changes in cross-referenced schemas no longer leave generated sources stale;
- performance fix: `GET /api/rbac/context` and `GET /api/hs/accounts/current` now load the own
  subject via a non-restricted query, avoiding a full grant-graph scan that took ~10s for global admins;
- Matrix alerting for `tools/login-monitoring-check` (`--matrix-webhook`, backoff via
  `HSADMINNG_LOGIN_MONITOR_ALARM_REPEAT`, `RESOLVED` message), deployed as a systemd user
  timer on the backend server via the new `remote install-monitoring` command;
- `remote deploy` now builds the jar locally and uploads it (activated by an atomic rename)
  instead of pushing the branch and building on the server; it verifies the built version info
  and reports the running version via `/actuator/info` after the restart;
- new public `GET /api/version` endpoint exposing build information
  (`version`, `buildTime`, `buildHost`, `gitBranch`, `gitCommit`, `gitDirty`);
- JWT decoders now validate the issuer ("iss") on all branches (HMAC, JWKS, issuer-URI)
  and optionally the audience ("aud") via the new env-var `HSADMINNG_JWT_AUDIENCE`
  (see audit finding 2 below);
- new `tools/create-accounts-from-csv` to mass-create accounts for existing persons and
  Keycloak subjects from a CSV-file; re-runnable (already existing accounts are skipped);
  replaces the deleted interactive `tools/create-account-for-existing-person`;
- new `tools/smoke-test`: quick end-to-end check of the bootRun+LOGIN+HTTP tooling
  against a throw-away Docker-PostgreSQL on separate ports (see the README);
- increased subject realm-prefix length from 3–5 to 3–12 characters to accommodate longer partner prefixes.


## Running the App with a Real JWT from the HS Keycloak

Supersedes the corresponding sections of the
[PR#220 document](2026-03-29-PR%23220-Fix-exception-with-real-JWT-from-HS-Keycloak-OIDC.md);
also see the README chapter about real JWT-authentication and
[doc/environment-variables.md](../environment-variables.md).

The realm for local development against the HS Keycloak changed from `testui` to `hostsharing`.

### `HSADMINNG_JWT_JWKS_URL` is not Supported Anymore

`application.yml` formerly defaulted `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
to `${HSADMINNG_JWT_JWKS_URL:}`; with the env-var unset, Spring-Security picked up the resulting
empty string as an invalid JWKS-URL. The property is now only set directly in the `fake-jwt`
profile; with a real JWT, the JWKS-URL is autodetected from `HSADMINNG_JWT_ISSUER`.

**Attention**: When running in Spring profile `fake-jwt`, the built-in `/fake-jwt/token` serves the JWT, no Kecloak involved.

### Complete Environment to Start the Application

```
source .unset-environment
# make sure no SPRING_... settings are set in the environment, e.g.:
unset SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI

export HSADMINNG_POSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/postgres
export HSADMINNG_POSTGRES_ADMIN_USERNAME=postgres
export HSADMINNG_POSTGRES_ADMIN_PASSWORD=password
export HSADMINNG_POSTGRES_RESTRICTED_USERNAME=restricted
export HSADMINNG_SUPERUSER=hsh-alex_superuser
export HSADMINNG_MIGRATION_DATA_PATH=migration
export HSADMINNG_OFFICE_DATA_SQL_FILE=

export HSADMINNG_JWT_ISSUER=https://login.dev.hsadmin.de/realms/hostsharing
export HSADMINNG_JWT_TOKEN_URL=$HSADMINNG_JWT_ISSUER/protocol/openid-connect/token

export LANG=en_US.UTF-8

export ALLOWED_ORIGINS=http://127.0.0.1:8082

gw bootRun --args='--spring.profiles.active=dev,complete,test-data'
```

For logging in (`LOGIN` function, direct password grant via `HSADMINNG_JWT_TOKEN_URL`,
client `hsscript.ng`), see [doc/environment-variables.md](../environment-variables.md).

## Audit Findings

Originally raised in the
[PR#238 audit](2026-07-03-PR%23238-sync-user-and-group-subjects-from-keycloak.md) (2026-07-04)
and moved here because they concern JWT validation and authentication in general rather than
the subject-sync API. They still matter for PR#238's security model: its global-admin-gated
sync endpoints are only as trustworthy as the token validation and handling addressed here.

### 1. The global-admin bearer token is persisted into the audit log on every call — FIXED

`Context.toCurl()` reconstructs the incoming request and stores it into `base.tx_context`
on every mutating transaction, formerly including the `Authorization` header — so a reusable
global-admin token landed in a table readable by DBAs/backups/replicas on every mutating call.

Fixed on this branch: credential-carrying headers (`authorization`, `cookie`,
`hostsharing-api-key`, `x-api-key`) are masked via `Context.HEADERS_TO_MASK`, verified by
`ContextUnitTest`. The request-*body* masking of sensitive properties was already addressed
on the PR#238 branch.

### 2. JWT decoders perform no audience validation; HMAC/JWKS branches perform no issuer validation — FIXED

Originally, only the `issuer-uri` decoder branch of `BaseWebSecurityConfig` validated the
issuer, and no branch validated the audience — although `Context.define()` trusts the token's
`preferred_username`/`sub` to select the subject, so a token minted for another purpose under
a shared signing key was accepted.

**Fixed** (2026-07-15) in `BaseWebSecurityConfig.standardJwtDecoder(...)`,
verified by `BaseWebSecurityConfigUnitTest`:

- all decoder branches (HMAC, JWKS, issuer-URI) now validate the issuer ("iss") against
  the configured `HSADMINNG_JWT_ISSUER`;
- the audience ("aud") is only validated if the new env-var `HSADMINNG_JWT_AUDIENCE`
  (comma-separated, at least one must match) is set: real access tokens from `realms/hs`
  (checked 2026-07-15) only carry Keycloak's realm-wide default audience `account` (the client
  only appears in the `azp` claim), which would not prevent cross-client token replay;
  audience validation stays disabled until an audience mapper (e.g. `hsadmin-ng-api`) is
  configured in Keycloak for the API-calling clients and the env-var is set accordingly;
- the `fake-jwt`-profile decoder is intentionally unchanged.
