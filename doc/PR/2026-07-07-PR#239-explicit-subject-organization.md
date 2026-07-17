# PR#239: Explicit subject organization instead of mandatory name-prefix derivation

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/239

## The Problem

Since PR#234/PR#236, the organization (also called realm) of a subject was **tied to the subject
naming pattern**: it existed only implicitly, encoded as the name prefix — `xyz-alice` for USER
subjects, `/xyz-Team` for GROUP subjects — and was re-derived from the name wherever it was necessary.

For **USER subjects** that coupling is the actual problem: their naming pattern is a
presentation/convention decision that we might want to change, and maybe even in a way from which
the organization **cannot be derived anymore** (e.g. names without a realm-prefix, or a grammar
where the prefix is optional or ambiguous). Users are identified by UUID everywhere — the JWT
references its user in the `sub` claim by UUID — so nothing but the naming convention ties a
user's organization to its name. As long as the organization is an artifact of the naming pattern,
every such change would silently break the organization semantics — most critically, the
organization-(realm-)based visibility policy from PR#236. The organization is a fact about the
subject and must be stored as such, independent of whatever the name happens to look like.
The main point is, nobody wants user names like `xyz-your.name` e.g. in Matrix or other media
which do not support UUID-based naming.

**GROUP subjects** are different: the JWT carries only the **group names** (at least by Keycloak
default), no UUIDs. A group can thus only be matched to its `rbac.subject` record by name, and
therefore the organization necessarily **is** the prefix of the group name right after the
leading `/`: `/example-Operators` → organization `example`. For groups the problem is not the
coupling itself — it cannot be removed — but that the strict `[a-z]{3,5}` realm-prefix grammar
rejects legitimate organization prefixes such as `example`.

Consequences of the coupling in the current code:

- The naming pattern had to be **enforced everywhere** just to keep the organization derivable:
  as OpenAPI name patterns (`^[a-z]{3,5}-[^/]+$` resp. `^/[a-z]{3,5}-[^/]+$`) and additionally as
  DB check constraints (`check_valid_user_subject_name`/`check_valid_group_subject_name` from
  PR#234). Keycloak user- and group-names which do not follow that grammar could not be
  synchronized at all.
- The visibility policy (PR#236) had to re-derive the organization from the name on every query,
  via non-sargable `LIKE concat(rbac.subject_realm_prefix(...), '-%')` patterns — a known
  performance and grammar-duplication issue, recorded as `TODO.impl[Taiga#471]` in
  `RealSubjectRepository`.
- The organization was not part of the API responses, so API clients had to re-implement the
  prefix-derivation — one more place hard-wired to the naming pattern.

## Non-Goals

This PR deliberately does **not**:

- introduce organizations as entities of their own (they remain plain values on subjects);
- migrate existing subject names (names remain unchanged; only the redundant validation is
  removed).

## The Scenarios

### Feature: Explicit subject organization

#### Background

- Keycloak remains the source of truth for users and groups; the sync program calls
  `PUT /api/rbac/subjects/{subjectUuid}` per change (PR#238).
- In the future, we want Keycloak user/group names which do not follow the realm-prefix grammar.
  Once we apply this naming policy change, the sync program will provide the organization explicitly.
- JWTs reference groups just by name (Keycloak default), no UUIDs. Thus, a GROUP subject's
  organization must remain derivable from the group-name prefix behind the `/`: an explicitly
  given GROUP organization is validated against that prefix (`/example-Operators` → organization
  must be `example`). USER subjects are referenced by UUID (`sub` claim), so their names stay
  free when an explicit organization is given.

#### Scenario#239.01: A global-admin can synchronize a USER subject with an explicit organization
<a id="scenario-239.01"></a>

so that Keycloak users whose names do not follow the realm-prefix grammar can be synchronized.

- **Given** the sync program is authenticated as a global-admin
  - **and** a Keycloak user with UUID `B1` and name `bob@example.com` in organization `example`
- **When** the sync program synchronizes the `USER` subject with the UUID `B1`, name `bob@example.com` and
  organization `example` via HTTP PUT
- **Then** the subject with UUID `B1` is created with the unchanged name `bob@example.com` and organization `example`
  - **and** the response returns `201 Created` and contains the organization

##### Verified by

- [SubjectSyncScenarioTests.aGlobalAdminCanSynchronizeAUserSubjectWithAnExplicitOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

#### Scenario#239.02: A global-admin can synchronize a GROUP subject with an explicit organization
<a id="scenario-239.02"></a>

so that Keycloak groups whose names do not follow the strict realm-prefix grammar (e.g. a prefix
longer than 5 letters) can be synchronized — while the organization stays derivable from the group
name, because JWTs reference groups just by name.

- **Given** the sync program is authenticated as a global-admin
  - **and** a Keycloak group with UUID `G2` and name `/example-Operators` in organization `example`
- **When** the sync program synchronizes the `GROUP` subject with name `/example-Operators` and
  organization `example` via HTTP PUT
- **Then** the subject is created with the unchanged name `/example-Operators` and organization
  `example`, matching the group-name prefix
  - **and** the response returns `201 Created` and contains the organization

##### Verified by

- [SubjectSyncScenarioTests.aGlobalAdminCanSynchronizeAGroupSubjectWithAnExplicitOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

#### Scenario#239.03: Without an explicit organization, the organization is derived from the name prefix
<a id="scenario-239.03"></a>

so that the sync program does not need to send the organization for realm-prefixed names, and all
PR#238 sync requests keep working unchanged.

- **Given** the sync program is authenticated as a global-admin
  - **and** a Keycloak user with UUID `C1` and the realm-prefixed name `xyz-carol`
- **When** the sync program synchronizes the `USER` subject with name `xyz-carol` and **no**
  organization via HTTP PUT
- **Then** the subject is created with the unchanged name `xyz-carol`
  - **and** the response returns `201 Created` and contains the derived organization
  - **and** fetching the subject by its UUID returns the stored organization `xyz`,
    derived from the name prefix

##### Verified by

- [SubjectSyncScenarioTests.aSubjectSynchronizedWithoutExplicitOrganizationGetsItDerivedFromTheNamePrefix](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

#### Scenario#239.04: A USER subject name starting with a slash is rejected despite an explicit organization
<a id="scenario-239.04"></a>

so that the leading `/`, which structurally marks GROUP subject names, cannot appear in USER
subject names even when no realm-prefix is required.

- **Given** the sync program is authenticated as a global-admin
- **When** the sync program tries to synchronize a `USER` subject with name `/bob` and organization
  `example` via HTTP PUT
- **Then** the request is rejected with `400 Bad Request`
  - **and** no subject is added or changed

##### Verified by

- [SubjectSyncScenarioTests.aUserSubjectNameStartingWithASlashIsRejectedDespiteAnExplicitOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

#### Scenario#239.05: A GROUP subject name without a leading slash is rejected despite an explicit organization
<a id="scenario-239.05"></a>

so that GROUP subject names structurally remain distinguishable from USER subject names even when
no realm-prefix is required.

- **Given** the sync program is authenticated as a global-admin
- **When** the sync program tries to synchronize a `GROUP` subject with name `example-Operators` and
  organization `example` via HTTP PUT
- **Then** the request is rejected with `400 Bad Request`
  - **and** no subject is added or changed

##### Verified by

- [SubjectSyncScenarioTests.aGroupSubjectNameWithoutALeadingSlashIsRejectedDespiteAnExplicitOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

#### Scenario#239.06: A GROUP organization which does not match the group-name prefix is rejected
<a id="scenario-239.06"></a>

so that a GROUP subject's organization always stays derivable from the group name —
JWTs reference groups just by name (Keycloak default), no UUIDs.

- **Given** the sync program is authenticated as a global-admin
- **When** the sync program tries to synchronize a `GROUP` subject with name `/example-Operators`
  and organization `xyz` via HTTP PUT
- **Then** the request is rejected with `400 Bad Request`, because the organization must be
  `example`, the group-name prefix
  - **and** no subject is added or changed

##### Verified by

- [SubjectSyncScenarioTests.aGroupOrganizationWhichDoesNotMatchTheGroupNamePrefixIsRejected](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

#### Scenario#239.07: Subjects can be listed filtered by their organization
<a id="scenario-239.07"></a>

so that API clients (e.g. admin UIs) can show the users and groups of one organization without
re-implementing any name-prefix knowledge.

- **Given** a USER `jane@example.com` with the explicitly given organization `smith`
  - **and** a GROUP `/smith-Operators`
  - **and** subjects of other organizations (e.g. `xyz-carol`, `hsh-alex_superuser`)
- **When** a user lists the subjects with `GET /api/rbac/subjects?organization=smith`
- **Then** the response contains at least the two above subjects of organization `smith`
  - **and** subjects of other organizations are not included

##### Verified by

- [SubjectSyncScenarioTests.subjectsCanBeListedFilteredByTheirOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [ListSubjectsFilteredByOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/ListSubjectsFilteredByOrganization.java)

#### Scenario#239.08: Re-synchronizing a USER subject with a different organization moves it to that organization
<a id="scenario-239.08"></a>

so that a user moved to another organization in Keycloak is reflected by the same idempotent
HTTP PUT, without needing a delete-and-recreate. (A GROUP subject's organization is bound to its
name prefix and thus can only change together with a matching rename.)

- **Given** a `USER` subject with UUID `B1`, name `bob@example.com` and organization `example`
  exists (from Scenario#239.01)
- **When** the sync program re-synchronizes the subject with the same UUID `B1` and name,
  but organization `acme`, via HTTP PUT
- **Then** the subject with UUID `B1` now has the organization `acme`
  - **and** its UUID and name remain unchanged
  - **and** the response returns `200 OK` (an update, not a creation)

##### Verified by

- [SubjectSyncScenarioTests.reSynchronizingAUserSubjectWithADifferentOrganizationMovesItToThatOrganization](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SubjectSyncScenarioTests.java)
  using the use-case [SynchronizeSubject](../../src/test/java/net/hostsharing/hsadminng/hs/rbac/scenarios/SynchronizeSubject.java)

## The Solution

The organization becomes a **first-class, separately stored property** of a subject, decoupled
from the naming pattern. Deriving it from the name prefix is kept only as a compatibility default,
so the USER naming pattern can evolve later — even into one from which no organization can be
derived — without touching the organization semantics or the visibility policy. GROUP names must
keep carrying the organization as their name prefix, because JWTs reference groups just by name
(Keycloak default), no UUIDs.

The subject sync API (`PUT /api/rbac/subjects/{subjectUuid}`) and the creation endpoint
(`POST /api/rbac/subjects`) accept a new **optional** `organization` property:

- If `organization` is **given** for a **USER** subject, it is stored as-is and the name is **not**
  parsed for a prefix. Name validation is then reduced to the structural minimum: USER subject
  names must not start with `/`.
- If `organization` is **given** for a **GROUP** subject, it is validated to match the group-name
  prefix behind the `/` (`/example-Operators` → organization must be `example`); this way group
  names beyond the strict `[a-z]{3,5}` realm-prefix grammar become possible while the organization
  stays derivable from the name.
- If `organization` is **not given**, everything stays as before: the name must match the strict
  realm-prefixed pattern, and the organization is derived from it — the part before the first `-`,
  without the leading `/` of GROUP names (`rbac.subject_realm_prefix(...)` resp.
  `Subject.organizationFromName(...)`).
- The **name remains unchanged** in all cases, even if the prefix determines the organization
  — the organization is stored separately and never rewrites the name.

The DB table `rbac.subject` stores the organization in its own (indexed, `not null`) column.
The organization is returned in the response JSON of all subject endpoints
(`GET/POST /api/rbac/subjects`, `GET/PUT …/{subjectUuid}`), and `GET /api/rbac/subjects`
supports an exact-match `organization` query-parameter filter (Scenario#239.07).

The DB-level naming-pattern validation is removed: the check constraints and their
`rbac.is_valid_user_subject_name`/`rbac.is_valid_group_subject_name` functions are dropped,
together with the `RbacTranslations` entries which mapped their PostgreSQL errors to translated
messages. Name validation now happens exclusively at the OpenAPI/controller level.

### Affected API Surface

- **Changed** `PUT /api/rbac/subjects/{subjectUuid}` and `POST /api/rbac/subjects` — the `anyOf`
  request body gains two new members `RbacUserSubjectWithOrganizationInsert` and
  `RbacGroupSubjectWithOrganizationInsert` (with a required, non-empty `organization` and the
  relaxed name patterns `^[^/].*$` resp. `^/.+$`), next to the unchanged
  `RbacUserSubjectInsert`/`RbacGroupSubjectInsert` without organization. An explicit GROUP
  organization must match the group-name prefix (400 otherwise).
- **Changed** `RbacSubject` response schema — now contains the required `organization` property;
  returned by all subject read and write endpoints.
- **Changed** `GET /api/rbac/subjects` — new optional `organization` query parameter which filters
  by the exact organization, combinable with the existing `name` and `type` filters; visibility
  rules are unaffected.

### Key implementation points

- **Controller dispatch** (`RbacSubjectController.toValidatedSubjectEntity`): the `anyOf` body is
  dispatched on the `type` discriminator **and** the presence of the `organization` key to one of
  the four generated resources; the OpenAPI constraints are applied via `StrictBodyConverter` as
  before, and an explicit GROUP organization is programmatically validated against the group-name
  prefix (a cross-property constraint the OpenAPI schema cannot express). The organization is
  always populated on the entity — explicitly or derived — so the response resource never needs a
  DB round-trip.
- **DB schema** (`1050-rbac-base.sql`): new column `rbac.subject.organization varchar(63)` with an
  index, plus a `before insert` trigger which defaults a null organization from the name prefix,
  keeping the legacy `rbac.create_subject(...)` callers (e.g. test-data generators) working.
- **Back-fill** (`1085-rbac-user-subject-names.sql`): pre-existing subjects get their organization
  back-filled from the name prefix **after** the PR#234 rename changesets, then the column becomes
  `not null`. The same changelog drops the name-check constraints and pattern functions.
- **DB-level upsert** (`rbac.upsert_subject`, `1055-rbac-views.sql`): gains the `newOrganization`
  parameter (old signature dropped); an update also updates the organization, so a later sync can
  change a subject's organization.
- **Views** (`1055-rbac-views.sql`): `rbac.subject_rv` and `rbac.subject_ev` are re-created via
  `runOnChange` to expose the new column; the `rbac.subject_rv` instead-of-insert trigger passes
  the organization through. The formerly non-repeatable `subject_ev` changeset was made repeatable
  (`runOnChange:true` + `--validCheckSum: ANY`) instead of adding a follow-up changeset.
- **Visibility** (`RealSubjectRepository.VISIBLE_SUBJECT_CONDITION`): the realm-prefix `LIKE`
  patterns are replaced by sargable equality on `s.organization`, both for the own-organization
  rule and the same-natural-person groups rule. This resolves the `TODO.impl[Taiga#471]`
  performance/grammar concerns. Note the semantics: a USER subject whose explicit organization
  differs from its name prefix is visible according to its **organization**, not its name
  (for GROUP subjects both always coincide).

### Impact on existing behavior and tests

- Formerly DB-rejected names (e.g. `invalid-user@example.com` as USER without organization) are
  still rejected — but now only by the OpenAPI validation; the corresponding
  `RbacTranslations`/repository tests for the DB constraint were replaced by organization-focused
  tests.
- The pattern-sync unit test now verifies that all four insert schemas' YAML name patterns match
  the generated bean-validation annotations (the SQL pattern comparison is gone with the SQL
  functions).
- New/changed coverage:
  - `RbacSubjectControllerRestTest` — dispatch and validation of the with-organization variants,
    empty-organization rejection, GROUP organization-vs-name-prefix mismatch rejection,
    organization in responses, organization list-filter delegation.
  - `RbacSubjectRepositoryIntegrationTest` — organization derivation via the DB default trigger,
    explicit organization with arbitrary names (USER and GROUP).
  - `RealSubjectRepositoryIntegrationTest` — `visibilityFollowsExplicitOrganizationInsteadOfNamePrefix`
    and `canFilterByOrganization`.
  - `RbacSubjectControllerAcceptanceTest` —
    `globalAdmin_canCreateANewUserWithExplicitOrganizationAndArbitraryName` 
    plus organization assertions in the existing creation tests.
  - `SubjectSyncScenarioTests` — Scenario#239.01–.08 above.

## Open Questions

- **Validation of the organization value itself:** only non-emptiness is enforced
  (`minLength: 1`); no grammar is imposed on explicit organizations, matching the DB which stores
  it as plain `varchar(63)`. Should a stricter pattern be required?
- **Changing the organization of an existing USER subject:** a repeated HTTP PUT updates the
  organization along with the name; there is no immutability rule like for `type` (current behavior
  specified and verified by Scenario#239.08; GROUP organizations are bound to the name prefix
  anyway). Should the USER organization be immutable instead?

## Prerequisite PRs

- PR#238: Synchronize Keycloak users and groups as ReBAC subjects.
