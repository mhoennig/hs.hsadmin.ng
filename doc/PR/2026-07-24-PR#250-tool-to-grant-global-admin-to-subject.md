# PR#250: Tool to grant global-admin to a single subject

Tooling only; no production code, no schema and no database change.

## Related Links (Hostsharing-internal)

- Taiga-Epic: https://plan.hostsharing.net/project/admin-hostsharing/epic/5972
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/250
- Extracted from PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/246

## The Problem

Granting the global-admin role to a subject required assembling the requests by hand:
looking up the role UUID, looking up the subject UUID, and posting the grant
with exactly one assumed role.

The following part is also easy to miss:
`POST /api/rbac/grants` books the grant as granted-by the currently assumed role,
so without `Hostsharing-Assumed-Roles: rbac.global#global:ADMIN` the request fails with
`[400] Granting roles to user is only possible if exactly one role is assumed`.

## The Solution

`tools/grant-global-admin-to-subject <subject-name>` resolves the role and the exactly named
subject, then grants `rbac.global#global:ADMIN` with assume,
authenticated with the stored `provisioning` API-key.

The subject lookup filters for an exact name match,
because the `name` query-parameter of `GET /api/rbac/subjects` is a prefix filter.

An already granted subject is answered with `409 Conflict` by the API,
which the tool reports and treats as success,
so the command can be re-run at any time.
Duplicate grants cannot occur anyway, as `rbac.grant` is unique on
`(ascendantUuid, descendantUuid)`.

## Merge Order

Independent of all other branches extracted from PR#246; can be merged at any time.
