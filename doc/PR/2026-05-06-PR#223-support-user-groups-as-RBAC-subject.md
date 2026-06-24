# PR#223: Support user groups as RBAC subject

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/223

## The Problem

The current RBAC model treats every `rbac.subject` as a user subject.
This means that roles can only be granted directly to individual users.
Hostsharing users are also organized in groups in Keycloak, but these groups cannot yet act as RBAC principals in hsadmin-NG.
As a result, permissions that should apply to a whole user group still have to be represented through individual user grants or other workarounds.

## The Solution

Groups are modeled as `rbac.subject` entries with type `GROUP`; existing user subjects become type `USER`.
Only `USER` subjects may be linked to `hs_accounts.account`.
Synchronized Keycloak groups can receive role grants, while group assignments remain runtime data from the JWT and are converted into effective subject UUIDs during context creation.

User group assignments only exist in the JWT and are converted to `GROUP` subject UUIDs at runtime.
Group assignments are therefore not stored in hsadmin-NG.

For the detailed design rationale, see [RFC: Groups as Subject Type in the ReBAC System](../rfc/2026-05-06-RFC%230001-rbac-subject-groups.md).
The RFC is currently written in German, but it is the canonical design reference for this PR.

## Prerequisite PRs

- Rename the HTTP header `assumed-roles` to `Hostsharing-Assumed-Roles`.
  The deprecated `assumed-roles` header remains supported as a runtime-only migration fallback.

## Implemented Changes

### Subject Model

- [x] Add a `type` property with the initial values `USER` and `GROUP` to the table `rbac.subject` and entity classes.
- [x] Restrict `hs_accounts.account` to `USER` subjects.
  `GROUP` subjects must not have an associated account.
- [x] Support groups as grantees in `POST /api/rbac/grants`.
  This was already covered by the current implementation.

### Runtime Context

- [x] Include group assignments from the JWT in the effective subjects of the RBAC context so users can receive roles indirectly through their groups.
- [x] For group paths, synchronized parent path subjects are included as effective subjects as well.
  This is supported by the runtime resolver for possible future nested Assembly groups; the current Assembly Realm does not synchronize nested groups.
- [x] For requests without `assumedRoles`, these group subject UUIDs are added to the effective subjects in addition to the user subject.
- [x] With `assumedRoles`, user and group subjects are used to authorize the assumed roles; afterwards, the session context continues to contain only the assumed role UUIDs.

### API Surface

- [x] Extend the `RbacSubject` API model with at least the subject `type`.
- [x] Extend `GET /api/rbac/subjects` with a `type=USER|GROUP` query parameter.
- [x] Extend `GET /api/rbac/subjects` so a subject can list synchronized `GROUP` subjects from its current group context.
  This is implemented by combining the RBAC-visible subjects with the current subject's group subjects from the real subject repository.
- [x] Extend `GET /api/rbac/roles` with an exact `name` query parameter so scenarios can resolve role UUIDs without listing all roles.

### Tests And Test Data

- [x] Add groups to the test data.
- [x] Add tests for context creation with multiple effective subject UUIDs.
- [x] Add tests for `assumedRoles` where the assumed role is reachable only through a group subject.
- [x] Add group-aware test support:
  - fake JWT bearers and scenario users can carry Keycloak groups,
  - RBAC test data contains synchronized, flat, customer-prefixed `GROUP` subjects such as `/xyz-Team` and `/xyz-Service`,
  - nested group-path expansion is covered by a context test with transaction-local subjects, not by global test data,
  - mass test data can create prefixed user and group subjects plus representative grants.

### Migration Compatibility

- [x] Adapt Liquibase migration compatibility for the changed context signature and repeatable views/functions.
  This includes keeping the base context procedure callable during migrations and renaming the statistics label from "login users" to "subjects".

## Follow-up PRs

1. Amend the test data subjects like "selfregistered-user-drew@hostsharing.org"
   to something like "xyz-drew" to match our visibility pattern.
2. Extend subject visibility so users can see all user and group subjects in their own realm.
3. Synchronize users and groups from Keycloak to hsadmin-NG.
4. Add a performance test or EXPLAIN-based check for the recursive grant CTE with multiple groups per user.
5. Support cross-realm grants and the required trust relationship between realms.

## Open Requirements and Design Issues

- Whether a login JWT may implicitly create an account is still undecided.
- The source of the `hs_office.persons` UUID for synchronized user accounts is still undecided.
- Cross-realm grants to groups remain a follow-up topic because visibility and trust between realms need a separate design.

## Findings

### Ambiguous _IdName_

The term `...IdName`, especially when compared to just `...Name` is confusing.
`...IdName` sounds more precise than `...Name`, but it's the other way around.
_"...IdName"_ is more like a volatile "display name", but it can be ambiguous;
it's mostly used for human-readability in testing and debugging.
Where _"...Name"_ contains a UUID for the referenced object. 

Example:
- RoleIdName: "hs_hosting.asset#fir01:AGENT"
- RoleName: "hs_hosting.asset#e60bdead-9a15-404d-a430-335310402480:AGENT"

Unfortunately, to change that naming all the way down to the database, it would require a major database migration.

## Unrelated Changes

<!-- this section contains cleaups or small bugfixes which are not actually part of the PR topic -->

- Scenario-test reporting and helper APIs were cleaned up to support the new group scenario:
  - generated scenario reports now render requests in an `HTTP METHOD ...` command style with one `-H` line per header,
  - report headers are ordered for readability: authorization first, assumed roles second, and content type directly before the request body,
  - fake bearer tokens in reports are shown as `Bearer JWT { ... }` with only the relevant claims (`sub` and, if present, `groups`),
  - `sub` values are rendered as `uuid<subject-name>` to make clear that a real JWT would carry a UUID while keeping the report readable.
- `AGENTS.md` was extended with Liquibase and scenario-test conventions.
- A brittle booking-project list acceptance test was removed, and a migration import setup was simplified.
- The unused `bin/git-watch-origin-and-test` file was removed (rebasing aftermaths).
- Change the RBAC role API response contract so `roleName` is unambiguous and UUID-based, for example
  `hs_booking.project#055df25f-f43e-4cc6-a3a8-e6b283e55514:ADMIN`.
  The previous symbolic/id-name-based value is still returned as `roleIdName`, for example
  `hs_booking.project#D-1000111-D-1000111defaultproject:ADMIN`.
  This applies to all responses using `RbacRole`, including `GET /api/rbac/roles` and the `assumedRoles` section of `GET /api/rbac/context`.
  The `GET /api/rbac/roles?name=...` lookup remains backward-compatible and accepts both forms.
- The subject `type` property was moved from `RbacSubjectEntity` to the shared `Subject` base class so RBAC and real subject entities expose the same subject type mapping.

## Attachments

- [RFC: Gruppen als Subject-Typ im ReBAC-System](../rfc/2026-05-06-RFC%230001-rbac-subject-groups.md)
