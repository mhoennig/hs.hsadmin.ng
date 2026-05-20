# PR#224: Do not require debitor UUID anymore to fetch booking projects

## The Problem

The booking project list endpoint required a `debitorUuid` query parameter.
This made it impossible to fetch all booking projects that are visible to the current subject in a single request.
Clients which only need the visible projects first had to know a debitor UUID before calling `/api/hs/booking/projects`.
That's inconvenient for users and also makes Scenario-Tests confusing.

## The Solution

The `debitorUuid` query parameter of `/api/hs/booking/projects` is now optional.
If `debitorUuid` is provided, the endpoint keeps the existing behavior and returns the visible booking projects for that debitor.
If `debitorUuid` is omitted, the endpoint returns all booking projects visible to the current subject or its assumed roles.

The OpenAPI definition was updated by renaming the operation to `getListOfBookingProjectsByOptionalDebitorUuid`.
The booking project repository now exposes `findAll()` for the unfiltered visible-project lookup.
The controller chooses between `findAllByDebitorUuid(...)` and `findAll()` depending on whether the query parameter was provided.

An acceptance test was added for fetching all visible booking projects without `debitorUuid`.

## Additional Changes

As we currently have no CI/CD in place, I found some failing tests due to changes in previous PRs:

### Follow-up to PR #221: Project Visibility Through `REFERRER`

PR #221 ("Taiga459: make projects visible to debitor despite unassumed grant")
changed the booking project RBAC model by introducing the project `REFERRER` role for `SELECT` permission.
The branch adjusts tests which still expected the old role graph:

* `HsBookingProjectRbacEntityUnitTest`
* `HsBookingItemRbacEntityUnitTest`
* `HsBookingProjectRepositoryIntegrationTest`

### Follow-up to PR #216: Global Admin RBAC Checks

PR #216 ("avoid-recursive-rbac-query-for-global-admins in the _rv generator") changed how global-admin visibility and assumed roles interact.
The branch contains related fixes:

* `rbac.isGlobalAdmin()` is documented as subject-based and independent of assumed roles.
* `rbac.hasGlobalAdminRole()` now represents the effective RBAC context: true for a global admin without assumed roles or with the global admin role assumed, false when only non-global roles are assumed.
* Generated insert permission checks now use `rbac.hasGlobalAdminRole()` instead of `rbac.isGlobalAdmin()`.
* Generated RBAC trigger changesets are marked `runOnChange:true validCheckSum:ANY` and use `create or replace trigger`.
* Affected generated RBAC changelog files were regenerated.
* `ContextIntegrationTests` and `TestPackageRepositoryIntegrationTest` were adjusted to the corrected effective global-admin behavior.

### RetroactiveTranslatorWithPlaceholderSupport

The new interface `RetroactiveTranslatorWithPlaceholderSupport` adds functionality for
retroactive translation support with placeholders.

This allows parsing values from actual error messages to use them in placeholders in translated messages.
A new HOWTO was also added.

### Other Fixed Issues

* `SystemProcessUnitTest` now checks stable message fragments because the exact `IOException` message varies between Linux versions.
* Upgrade to Testcontainers 1.21.4 to be compatible with recent Docker API versions.
* Added `bin/system-summary` to print a concise hardware, Linux, Docker, Java RE and system Gradle summary for documenting test environments.
* `gw-test` now prints a timestamp after each `RUNNING gw ...` headline and `DONE gw ...` summary.
* Improvements to the README.md:
  * export DOCKER_HOST
  * MacOS builds are no longer officially supported.
