# PR#236: Taiga#471: Realm-prefix-based user and group subject visibility

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/236

## The Problem

A `rbac.subject` can be a `USER` or a `GROUP` (and later also `APIKEY`).

User subjects use realm-prefixed names such as:

```text
xyz-some_user
```

Group subjects use Keycloak-style group names with a leading slash, for example:

```text
/xyz-admins
```

Currently (before this PR), the subject list API exposes
- if the current login user is a global admin, all subjects,
- for other users, the union-set of two sources:
  1. every subject that is a grantee of a role the current login may see,
     and (if no role is assumed), the login itself
  2. all group-subjects from the current login's JWT.

But we want the visibility rules as follows:

- By default, a user sees every user and group that belongs to their own organization (realm),
  identified by the subject-name prefix, so they can assign their own roles within that
  organization ([Scenario#236.01](#scenario-236.01)).

- Beyond their own realm, a user additionally sees all groups of any other organization in
  which the same natural person holds another user account ([Scenario#236.03](#scenario-236.03)).

- Groups assigned to a user must also be visible, but currently, 
  these always belong to the user's own realm, so they are visible via the realm prefix anyway.

- Apart from the same-person rule below, no subject of another organization is visible
  ([Scenario#236.04](#scenario-236.04)).

- When a ReBAC role is assumed, this behavior changes to stay consistent with the purely ReBAC-based APIs as follows:
   - Once a user assumes any non-global role, all subject-derived visibility is dropped
     and no subject is listed at all ([Scenario#236.05](#scenario-236.05)).
   - In contrast, assuming the global admin role makes all subjects visible ([Scenario#236.06](#scenario-236.06)).

- A global admin who has not assumed any role sees all subjects of all organizations ([Scenario#236.08](#scenario-236.08)).

- The optional `name` and `type` request filters only narrow the otherwise-visible set; they can
  restrict the result but never widen it to expose subjects that would not be visible without them
  ([Scenario#236.07](#scenario-236.07)).

Examples:

- `xyz-somebody` with no other account should see `xyz-some_user` but not `abc-some_user`.
- `xyz-somebody` with no other account should see `/xyz-admins` but not `/abc-admins`.
- `xyz-somebody` whose natural person has another account `abc-somebody` should see `/abc-some_group` but not `abc-some_user`.

Visibility here means only that records are returned by the relevant API endpoints,  mostly `/hs/rbac/subjects`.
This kind of visibility bypasses the ReBAC grant graph.
**Visibility of a subject does not imply any permission that those other subjects have.**

## The Requirements

### Feature: Visibility of subjects within an organization

#### Background

- Users and groups are defined in Keycloak and get synced as ReBAC subjects in this backend.
- The expression "can see a subject" means that this subject is returned by the REST endpoint `/api/rbac/subjects`.
- The REST endpoint `/api/rbac/subjects` lists all visible subjects.
- The organization is currently identified by a prefix in subject names, like the "xyz" in "xyz-username" or "/xyz-groupname".
- This organization information might later be in a separate property during sync, in JWTs, and in a new column in the table `rbac.subjects`.
- Each user and each group belongs to exactly one organization.
- Users can be assigned to groups, but only to groups of their own organization:
  the original realms cannot see each other, and group assignments stem from
  the original realms and are just mirrored into the assembly realm.
- Exactly one person is associated with each user, while a single person can be associated with multiple users.

#### Scenario#236.01: A user can see all subjects of the same organization
<a id="scenario-236.01"></a>

so that users can assign their own roles to users and groups of their own organization.

- **Given** a user belongs to the organization "xyz"
  - **and** other users and groups also belong to the organization "xyz"
- **When** the user requests the list of visible subjects
- **Then** all users of the organization "xyz" are visible
  - **and** all groups of the organization "xyz" are visible

##### Verified by

- [AccountScenarioTests.userCanViewSubjectsFromTheirOwnRealm](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ViewRbacSubjects.java)

#### Scenario#236.02: Users can see all groups assigned to them
<a id="scenario-236.02"></a>

so that users can assign their own roles to the groups they are assigned to.

- **Given** a user is assigned to one or more groups (of their own organization)
- **When** the user requests the list of visible subjects
- **Then** all groups assigned to the user are visible (as part of their organization's subjects)

As we realized that currently, users can only be assigned to groups with the same prefix;
thus within the same organization, this is the same as *Scenario#236.01*.

##### Verified by

- [AccountScenarioTests.userCanViewSubjectsFromTheirOwnRealm](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ViewRbacSubjects.java)

#### Scenario#236.03: Users can see subjects from other organizations through other accounts associated with the same person
<a id="scenario-236.03"></a>

so that users can grant their own roles even to groups of other organizations
with which their natural person is also associated.

- **Given** the user account "xyz-alice" is associated with the person "Alice"
  - **and** the user account "abc-alice" is also associated with the same person "Alice"
- **When** the user account "xyz-alice" requests the list of visible subjects
- **Then** the group "/abc-Team" is included

##### Verified by

- [AccountScenarioTests.userCanViewGroupsAssociatedWithAnotherAccountOfTheSamePerson](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjectsAssociatedWithSamePerson](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/ViewRbacSubjectsAssociatedWithSamePerson.java)

#### Scenario#236.04: Users can NOT see arbitrary subjects
<a id="scenario-236.04"></a>

so that internals of unrelated subjects do not get exposed to users.

- **Given** a user does NOT belong to the organization "abc"
  - **but** other users and groups belong to the organization "abc"
- **When** the user requests the list of visible subjects
- **Then** no subject of the organization "abc" is included

##### Verified by

- [AccountScenarioTests.userCanViewSubjectsFromTheirOwnRealm](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ViewRbacSubjects.java)

#### Scenario#236.05: Assuming a non-global role drops subject visibility stemming from the user and its groups
<a id="scenario-236.05"></a>

so that this API stays consistent to purely ReBAC-based APIs
in which the concrete subject also does not contribute any rights anymore once an accessible role got assumed.

- **Given** a user assumes a non-global ReBAC-role
- **When** the user requests the list of visible subjects
- **Then** no subject gets listed

##### Verified by

- [AccountScenarioTests.assumingANonGlobalRoleDropsAllSubjectVisibility](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ViewRbacSubjects.java)

#### Scenario#236.06: Assuming the global admin role keeps global subject visibility
<a id="scenario-236.06"></a>

so that this API stays consistent to purely ReBAC-based APIs
in which the global-admin role can see everything.

- **Given** a global admin assumes the ReBAC-role "rbac.global#global:ADMIN"
- **When** the user requests the list of visible subjects
- **Then** all subjects are visible

##### Verified by

- [AccountScenarioTests.assumingTheGlobalAdminRoleKeepsGlobalSubjectVisibility](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ViewRbacSubjects.java)

#### Scenario#236.07: Name and type filters narrow the visible subjects
<a id="scenario-236.07"></a>

so that clients can search within their visible subjects without ever widening visibility.

- **Given** a user who belongs to an organization which also comprises other subjects
  - **and** whose person is associated with another user-account of another organization
- **When** the user requests the list with a `name` and/or `type` filter
- **Then** only the matching subset of the otherwise-visible subjects is returned
  - **and** the filters can never add subjects that would not be visible without them

##### Verified by

- [AccountScenarioTests.nameAndTypeFiltersNarrowButNeverWidenVisibleSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjectsNarrowedByFilters](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/ViewRbacSubjectsNarrowedByFilters.java)

#### Scenario#236.08: A global admin without an assumed role sees all subjects
<a id="scenario-236.08"></a>

so that this API stays consistent to purely ReBAC-based APIs
in which the global-admin role can see everything.

- **Given** a global admin has not assumed any role
- **When** the user requests the list of visible subjects
- **Then** all subjects of all organizations are visible

##### Verified by

- [AccountScenarioTests.globalAdminWithoutAssumedRoleSeesAllSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/accounts/scenarios/AccountScenarioTests.java)
  using [ViewRbacSubjects](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ViewRbacSubjects.java)


## The Solution

Subject visibility for `GET /api/rbac/subjects` and `GET /api/rbac/subjects/{uuid}` is computed
completely in SQL: the shared predicate `RealSubjectRepository.VISIBLE_SUBJECT_CONDITION` contains
the whole policy including the role-assumption gates, and is used by both the list query and the
single-subject query — so every subject returned by the list can also be fetched by its UUID,
and no other subject can.
The ReBAC grant graph is never consulted: our ReBAC-subjects are not ReBAC-objects, thus they
have no roles, and subject visibility is independent of role grants.

The main solution is the SQL query `VISIBLE_SUBJECT_CONDITION`,
it parts map to the scenarios as follows:

- Same-realm subjects (Scenario#236.01, #236.02, #236.04)
  → `name like '<prefix>-%' or name like '/<prefix>-%'`, with the prefix derived by
  `rbac.subject_realm_prefix(name)` = `split_part(ltrim(name, '/'), '-', 1)`,
  so both `xyz-somebody` and `/xyz-admins` resolve to `xyz`.
  JWT groups need no visibility source of their own, as they always belong to the user's own realm.
- Same-person `GROUP` subjects in other realms (Scenario#236.03)
  → an `exists` sub-select reaching the other accounts of the same natural person
  via a `hs_accounts.account` join on `person_uuid`, not via the subject name.
- Role-assumption gates (Scenario#236.05, #236.06, #236.08)
  → `rbac.hasGlobalAdminRole()` and `not base.hasAssumedRole()`.
- `name` and `type` request filters (Scenario#236.07)
  → additional conjunctive predicates in the list query; they can never widen visibility.

`RbacSubjectController` delegates both endpoints to `RbacSubjectListService`, which uses these
`RealSubjectRepository` queries. The previous grant-based list query
`RbacSubjectRepository.findByOptionalNameLikeAndOptionalType(...)` (backed by `rbac.subject_rv`)
had no remaining production usage and was removed.
The other `/api/rbac/subjects/*` endpoints and the grant APIs are unaffected by this change.

Performance: an index on `rbac.subject.name` supports the prefix `like` patterns.
If that turns out to be insufficient, the prefix could be extracted into a new `organization`
column on `rbac.subject` at insert time — also future-proof, as the organization might later
arrive in the JWT (see Non-Goals / PR#238).

## Additional Changes

These changes are part of this PR but are not driven by the Gherkin scenarios above:

- `GET /api/rbac/context` now returns the caller's GROUP assignments split into two arrays
  (`RbacContextController` + the new `RbacSubjectGroup` schema):
  `claimedGroups` (the GROUP names claimed in the JWT, independent of any assumed role and
  including groups not yet synchronized as subjects) and `effectiveGroups` (the synchronized
  GROUP subjects that remain effective after applying assumed roles).
- Scenario-test harness: `UseCase.using(name, value)` captures setup-only properties
  that should not appear in the Given/Expected sections of the scenario report.
- All SQL functions that are pure functions of their arguments are now declared `immutable`
  (same arguments → same result forever) instead of `stable` or the default `volatile`;
  semantics are unchanged. The affected changesets were made repeatable
  (`runOnChange:true` + `--validCheckSum: ANY`) and amended in place; the `create type` in
  `rbac-base-ROLE-DESCRIPTOR` became a guarded creation ignoring `duplicate_object`,
  as PostgreSQL has no `create type if not exists`.

## Non-Goals

This PR deliberately does **not**:

- model same-realm visibility through ReBAC grants;
- change role assumption rules;
- implement cross-realm trust or cross-realm grants;
- change subject-name validation from PR#234;
- change Keycloak synchronization;
- handle legacy pre-PR#234 subject names (e.g. `test-user@example.org` resolving to the accidental
  realm prefix `test`): irrelevant, because production only contains a few admin accounts,
  all of which get migrated to the PR#234 naming pattern before this feature goes live;
- extract the organization from the subject-name prefix on ingesting the subjects
  (planned for PR#238; until then we have quite ugly SQL queries on table `rbac.subject`).

## Prerequisite PRs

- PR#223: Support user groups as RBAC subject.
- PR#234: Prefixed user subject names instead of email addresses.

## Follow-up PRs

1. Synchronize users and groups from Keycloak to HSAdmin NG using the subject-name pattern from PR#234.
2. Design cross-realm grants and the required trust relationship between realms.
