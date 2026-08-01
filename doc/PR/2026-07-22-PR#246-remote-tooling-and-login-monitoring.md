# PR#246: Remote-admin tooling and login-monitoring alerting

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

Operating the backend, monitoring, and the Keycloak sync need server-side tooling.
Deploys, service lifecycle, log access, DB access, and API-key provisioning
were partly manual and partly scattered over ad-hoc SSH commands.
The login monitor only detected hard login failures.
It raised no alert for latency degradation, for unexpectedly gained privileges,
or for a stalled Keycloak sync.

## The Solution

- `tools/login-monitoring-check` alerts by severity:
  ⚠️ SLOW when a successful check exceeds `HSADMINNG_LOGIN_MONITOR_SLOW_WARNING_SECONDS` (default 2s),
  🚨 FAILED on login/backend/sync errors,
  and 🆘 CRITICAL with an `@room` ping when the monitoring user unexpectedly gained privileges.
- It also checks sync-freshness: it polls the public `GET /api/rbac/subjects/last-sync` (from PR#258)
  and fails when the last run is older than `HSADMINNG_LOGIN_MONITOR_SYNC_MAX_AGE_SECONDS`
  (default 300s; `0` disables).
- Each alarm backs off independently via its own state file and posts a `RESOLVED` message when it clears.
- `tools/remote` now groups its commands as `remote <target> <action>`:
  lifecycle actions for `backend` and `login-monitor`,
  and `backend sql`/`sql-tunnel` for remote DB access with the backend's own credentials.
  `backend deploy` streams the startup log until the application reports it has started.
- `tools/http` defaults `-H 'Content-Type: application/json' --data-binary @-` for POST/PUT/PATCH,
  so a JSON body can be piped in without boilerplate options;
  the scenario-test reports (`UseCase`) drop the now-defaulted options from the generated HTTP commands.
- `tools/create-api-key-for-keycloak-sync` supports `.`-separated API-key names with an explicit organization;
  `tools/lib/apikeys-file.py` allows `.` in API-key names.
- `tools/smoke-test` also exercises `tools/create-accounts-from-csv` and `tools/create-api-key-for-keycloak-sync`;
  the new `HSADMINNG_SKIP_ENVIRONMENT_FILE` guard keeps the personal `.environment`
  from overriding the smoke-test environment in these child-process tools.
- The new Gradle task `smokeTest` wraps `tools/smoke-test`;
  the `.gitTally` CI build command runs it as its last task,
  and the CI build image (`Jenkins/jenkins-agent/Dockerfile`) additionally installs `jq` and `python3` for it.
- `README.md` and `doc/environment-variables.md` document the new commands and variables.

## Additional Changes

- The description of the `assumedRoles` header parameter in
  [auth.yaml](../../src/main/resources/api-definition/auth.yaml) now documents the role name format.
- [application.yml](../../src/main/resources/application.yml) defined `hsadminng.cors.allowed-origin`,
  but [BaseWebSecurityConfig](../../src/main/java/net/hostsharing/hsadminng/config/BaseWebSecurityConfig.java) reads
  `allowed-origins`, thus `ALLOWED_ORIGINS` had no effect at all;
  it is now `allowed-origins: ${ALLOWED_ORIGINS:*}`,
  where the `*` default keeps local development and the test suites functional.


## Follow-up PRs

- This PR and some predecessors, which actually got extracted from a former version of this PR,
  introduced some security issues that we found in a security audit.
  These issues will be addressed in the follow-up PR#254 and PR#260.
