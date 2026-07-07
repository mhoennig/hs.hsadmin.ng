# PR#238: Synchronize Keycloak users and groups as ReBAC subjects

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/238

## The Problem

In our architecture, the Assembly realm in Keycloak is the primary system for users and groups.
This backend needs to mirror them as `rbac.subject` records of type `USER` and `GROUP`.

This affects the creation, update, and deletion of subjects.

### Status quo

There already is a subject-creation endpoint, but it was built for self-registration, not for
admin-driven synchronization:

- `POST /api/rbac/subjects` (`RbacSubjectController.postNewSubject`, `rbac-subjects.yaml`)
  - creates exactly **one** subject at a time from a `RbacSubjectInsert` body
    (`anyOf` `RbacUserSubjectInsert` / `RbacGroupSubjectInsert`);
  - the request body already enforces the PR#234/PR#236 realm-prefixed name patterns
    (`^[a-z]{3,5}-[^/]+$` for `USER`, `^/[a-z]{3,5}-[^/]+$` for `GROUP`), and `type` defaults to `USER`;
  - is annotated **`@PreAuthorize("permitAll()")`** and runs with **`context.define(null)`**,
    i.e. it is currently reachable **unauthenticated** and creates the subject with a `null`
    current-subject context. This was behaviour was meant for self-registration; this PR replaces it with
    global-admin-only creation (see [Affected API Surface](#the-solution) and the renamed
    `RbacSubjectControllerAcceptanceTest.CreateRbacSubject.globalAdmin_can…` tests).

So functionally the create endpoint already does what a sync needs (create a `USER` or a `GROUP`
subject by realm-prefixed name), **but its authorization model does not match the sync use case**:
it neither requires authentication nor verifies that the caller is a global-admin, and it does not
establish a caller context at all.

The complementary endpoints already exist but do not fully match the sync use case:

- `DELETE /api/rbac/subjects/{subjectUuid}` (`deleteSubjectByUuid`) — currently assumes the caller's roles
  from the JWT and lets the DB trigger authorize the delete (self or global-admin).
  It currently performs a **physical delete** (`rbac.delete_subject_tf` runs `delete from rbac.subject`,
  `1055-rbac-views.sql:303`), whereas the sync use case needs a **soft-delete/deactivation**.
- `GET /api/rbac/subjects` and `GET /api/rbac/subjects/{subjectUuid}` — the visibility-filtered
  read endpoints from PR#236; they do not yet know about deactivation.

There is yet **no** endpoint to **update** an existing subject (e.g. after a Keycloak rename), and **no**
soft-delete/deactivation concept: `rbac.subject` has only `uuid`, `name`, `type`
(`1050-rbac-base.sql:42`, plus later `alter table`s), with no `active`/`deactivated` column.

There is also **no** dedicated Keycloak/sync endpoint and **no** separate `users`/`groups` resources:
one unified subjects endpoint handles both types via the `anyOf` body.

Note on identity: the create endpoint already accepts an **optional** `uuid` in the body (it only
generates a random UUID when none is given), so the same UUID that Keycloak assigns can already be
stored in `rbac.subject.uuid`. This PR relies on that: the UUID is the stable identity shared with
Keycloak, while the `name` is mutable.

## The Solution

**Operational model**

- An external sync program subscribes to Keycloak events for the Hostsharing Assembly realm and calls
  this backend once per added, renamed, or removed user/group.
- It authenticates with a JWT carrying the global-admin role (`rbac.global#global:ADMIN`), required to
  manage subjects across all realm-prefixes.
- Each subject is identified by its **UUID** (the same one Keycloak uses); only its **name** is mutable.
- Only `USER` and `GROUP` **subjects** are synchronized — **not** user-accounts (which also need a
  person) and **not** group membership (see [Non-Goals](#non-goals)).

**Operations** (concrete endpoints and status codes in the Affected API Surface below)

- **Upsert** — one idempotent, UUID-keyed operation both **adds** a subject and **updates its name**,
  because the sync program cannot know whether the backend already has it.
- **Remove** — a **soft-delete** (deactivation) by default, so a transient sync failure that replays a
  spurious removal does not lose grants; the record is retained for audit and reactivation. A physical
  purge is available for the rare irreversible case.
- **Authorization** — upsert, create, and delete are all **global-admin-only**; the former
  unauthenticated self-registration on `POST` is dropped (see [Open Questions](#open-questions)).

One unified subjects endpoint keeps handling both `USER` and `GROUP` via the existing `anyOf` body
and the PR#234/PR#236 realm-prefixed name patterns; no split `users`/`groups` resources are
introduced. Design rationale and the specific SQL changes are documented per finding under
[Audit Findings](#audit-findings).

### Affected API Surface

- **New** `PUT /api/rbac/subjects/{subjectUuid}` — global-admin-only idempotent upsert; creates on an
  unknown UUID (`201`), even one reusing a deactivated subject's name, updates the name / no-op on a
  known one (`200`), reactivates a deactivated subject with the same UUID (`200`), `409` on an
  active-name collision, `400` on an invalid name/body (Scenario#238.01–.03, .07–.09). The UUID comes
  from the path.
- **Changed** `DELETE /api/rbac/subjects/{subjectUuid}?purge={true|false}` — now global-admin-only
  (`401` unauthenticated, `403` non-admin); default **deactivates** (soft-delete), `purge=true`
  **physically removes** the subject and its grants. `204` in both modes, idempotent for an unknown or
  already-deactivated subject.
- **Changed** `GET /api/rbac/subjects` and `GET …/{subjectUuid}` — now exclude deactivated subjects;
  PR#236 visibility rules otherwise unchanged.
- **Changed** `POST /api/rbac/subjects` — now global-admin-only (`401`/`403`); no longer runs
  unauthenticated with a `null` caller context.

### Key implementation points

- **Deactivation** adds a nullable `rbac.subject.deactivated_at` column and is an explicit repository
  `UPDATE`, not a repurposed delete; it is filtered out on read **and** authorization paths, so a
  deactivated subject can no longer authenticate or resolve group roles (audit finding 1).
- **Name uniqueness** is relaxed to a partial index `unique (name) where deactivated_at is null`, so a
  name freed by a deactivated subject can be reused under a fresh Keycloak UUID (audit finding 3,
  verified by Scenario#238.08).
- **Validation** dispatches the `anyOf` body on the `type` discriminator to the generated DTOs and
  validates programmatically (name patterns, required fields, `additionalProperties: false`) before
  any SQL — on `POST` and `PUT` alike (audit finding 2).
- **Purge** reuses the existing cascade/trigger machinery; the account FK is made `on delete cascade`
  (audit finding 4) and the DB-side self-or-global-admin check moves into the `BEFORE DELETE` trigger
  (audit finding 5). No new RBAC-engine SQL is required.

### Impact on existing behavior and tests

- Delete acceptance tests now assert **deactivation** (subject retained but not visible/listed), not
  physical deletion.
- Authorization is **tightened**: a subject can no longer soft-delete itself
  (`nonGlobalAdmin_canNotDeleteTheirOwnUser`), and `POST` requires global-admin (anonymous → `401`,
  non-admin → `403`).
- Both delete modes and the upsert are covered by `RbacSubjectControllerAcceptanceTest`,
  `RbacSubjectControllerRestTest`, and `RbacSubjectRepositoryIntegrationTest` (`DeactivateSubject`
  soft-delete/idempotent, `DeleteSubject` physical cascade).

### Open Questions
<a id="open-questions"></a>

- **PUT on a deactivated UUID:** if the same UUID reappears in Keycloak, should `PUT` reactivate the
  subject, or is that a `404`/`409`? (Keycloak normally assigns a fresh UUID on re-create, so this is
  an edge case.) Proposed default: `PUT` reactivates and updates the name.
- **Fate of `POST`:** `POST` is now restricted to global-admins (aligned with
  `PUT`/`DELETE`) and the unauthenticated self-registration path is dropped.
  Later we might need some sort of initial-global-admin registration to be able to start from scratch,
  where this might be useful, so we keep it for now.
- **Purging deactivated subjects:** on-demand physical purge is now available via
  `DELETE …?purge=true`. Only the automated purge *job* — deciding which
  long-deactivated subjects to purge and when — remains out of scope and is left to a follow-up.
- **Dedicated sync role:** the sync program acts as a global-admin for now; a narrower,
  purpose-specific role may be introduced later.

## The Requirements

### Feature: Synchronize Keycloak users and groups as ReBAC subjects

#### Background

- Keycloak is the source of truth for users and groups of our realm.
- An external sync program subscribes to Keycloak events and calls this backend's REST API for each
  added, renamed, or removed user or group.
- The sync program authenticates with a JWT carrying the global-admin role `rbac.global#global:ADMIN`.
- A subject is identified by its **UUID**, which is the same UUID Keycloak uses. The subject's
  **name** can change over time; the UUID never changes.
- `USER` subjects use realm-prefixed names such as `xyz-alice`; `GROUP` subjects use Keycloak-style
  names with a leading slash such as `/xyz-Team` (patterns from PR#234/PR#236).
- Each API call synchronizes exactly one subject.
- The sync program does not necessarily track whether this backend already knows a subject,
  so adding and renaming are one and the same idempotent upsert keyed by UUID.
- Removal is a **deactivation** (soft-delete): the subject is kept but no longer appears in queries.
- Only `USER` and `GROUP` subjects are synchronized — neither user-accounts (which require a person)
  nor group membership.

#### Scenario#238.01: A global-admin can synchronize a new USER subject
<a id="scenario-238.01"></a>

so that a Keycloak user becomes assignable as a ReBAC subject in this backend.

- **Given** the sync program is authenticated as a global-admin
  - **and** a Keycloak user with UUID `U1` and name `xyz-alice` that does not yet exist as a subject
- **When** the sync program upserts the `USER` subject with UUID `U1` and name `xyz-alice`
- **Then** a `USER` subject with UUID `U1` and name `xyz-alice` exists in the backend
  - **and** the response returns `201 Created`

##### Verified by

- `SubjectSyncScenarioTests.aGlobalAdminCanSynchronizeANewUserSubject`

#### Scenario#238.02: A global-admin can synchronize a new GROUP subject
<a id="scenario-238.02"></a>

so that a Keycloak group becomes assignable as a ReBAC subject in this backend.

- **Given** the sync program is authenticated as a global-admin
  - **and** a Keycloak group with UUID `G1` and name `/xyz-Team` that does not yet exist as a subject
- **When** the sync program upserts the `GROUP` subject with UUID `G1` and name `/xyz-Team`
- **Then** a `GROUP` subject with UUID `G1` and name `/xyz-Team` exists in the backend
  - **and** the response returns `201 Created`

##### Verified by

- `SubjectSyncScenarioTests.aGlobalAdminCanSynchronizeANewGroupSubject`

#### Scenario#238.03: Re-synchronizing an existing subject updates its name idempotently
<a id="scenario-238.03"></a>

so that the sync program can propagate Keycloak renames and safely replay events, without needing to
know whether the subject already exists in this backend.

- **Given** a subject with UUID `U1` and name `xyz-alice` already exists
- **When** the sync program upserts the subject with the same UUID `U1` and the new name `xyz-alicia`
- **Then** the subject with UUID `U1` now has the name `xyz-alicia`
  - **and** no second subject is created
  - **and** the response returns `200 OK`
  - **and** upserting again with identical data leaves the subject unchanged (`200 OK`)

##### Verified by

- `SubjectSyncScenarioTests.reSynchronizingAnExistingSubjectUpdatesItsNameIdempotently`

#### Scenario#238.04: A non-global-admin cannot synchronize subjects
<a id="scenario-238.04"></a>

so that only the trusted sync program (acting as global-admin) can synchronize subjects.

- **Given** an authenticated user who does **not** hold the global-admin role
- **When** that user tries to upsert a subject
- **Then** the request is rejected with `403 Forbidden`
  - **and** no subject is added or changed

##### Verified by

- `SubjectSyncScenarioTests.aNonGlobalAdminCannotSynchronizeSubjects`

#### Scenario#238.05: A global-admin can deactivate a subject removed from Keycloak
<a id="scenario-238.05"></a>

so that subjects deleted in Keycloak stop being usable while their record is retained.

- **Given** the sync program is authenticated as a global-admin
  - **and** a subject with UUID `U` exists that was removed from Keycloak
- **When** the sync program deletes the subject with UUID `U`
- **Then** the subject is marked deactivated (not physically deleted)
  - **and** the response returns `204 No Content`

##### Verified by

- `SubjectSyncScenarioTests.aGlobalAdminCanDeactivateARemovedSubject`

#### Scenario#238.06: A deactivated subject no longer appears in queries
<a id="scenario-238.06"></a>

so that deactivated subjects can neither be listed nor assigned.

- **Given** a subject `xyz-alice` has been deactivated
- **When** any user requests the list of subjects (`GET /api/rbac/subjects`)
- **Then** `xyz-alice` is not included in the result

##### Verified by

- `SubjectSyncScenarioTests.aDeactivatedSubjectNoLongerAppearsInQueries`

#### Scenario#238.07: Invalid subject names are rejected
<a id="scenario-238.07"></a>

so that only realm-prefixed names as defined in PR#234 enter the subject table.

- **Given** the sync program is authenticated as a global-admin
- **When** the sync program tries to upsert a subject whose name does not match the required
  realm-prefix pattern
- **Then** the request is rejected with `400 Bad Request`
  - **and** no subject is added or changed

##### Verified by

- `SubjectSyncScenarioTests.invalidSubjectNamesAreRejected`

#### Scenario#238.08: A new subject with a new UUID is created despite a deactivated subject with the same name
<a id="scenario-238.08"></a>

so that a name freed by a deactivated subject can be claimed by a completely new Keycloak user —
Keycloak enforces unique usernames only among existing users, and this backend only among active
subjects.

- **Given** a subject with UUID `U1` and name `xyz-alicia` has been deactivated
- **When** the sync program upserts a subject with a new UUID `U2` and the same name `xyz-alicia`
- **Then** a new subject with UUID `U2` and name `xyz-alicia` exists in the backend
  - **and** the response returns `201 Created`
  - **and** the name refers to exactly one active subject
  - **and** the deactivated subject with UUID `U1` remains deactivated and untouched

##### Verified by

- `SubjectSyncScenarioTests.aNewSubjectWithANewUuidIsCreatedDespiteADeactivatedSubjectWithTheSameName`

#### Scenario#238.09: A deactivated subject is reactivated by the next synchronization with the same UUID
<a id="scenario-238.09"></a>

so that a replayed Keycloak event or a reappearing user/group restores the retained subject instead
of failing or creating a duplicate.

- **Given** a subject with UUID `U` has been deactivated
- **When** the sync program upserts a subject with the same UUID `U`
- **Then** the subject with UUID `U` is active again and visible in queries
  - **and** it keeps its UUID and carries the synchronized name (its original name `xyz-alice`,
    since a new subject claimed `xyz-alicia` in Scenario#238.08)
  - **and** the response returns `200 OK` (an update, not a creation)

##### Verified by

- `SubjectSyncScenarioTests.aDeactivatedSubjectIsReactivatedByTheNextSynchronizationWithTheSameUuid`

## Additional Changes

An audit was performend using Claude Fable 5.
The following additional changes stem from that audit report.

### fixing the request body in `Context.toCurl` was always empty

Not part of the sync feature itself, but noticed while testing the audit-log for the new subject
operations: the curl command reconstructed into `base.tx_context.currentRequest` never contained
a request body. `Context.toCurl` reads `request.getReader()`, but Spring's request-body handling
had already consumed the stream, and the previously used `ContentCachingRequestWrapper` does not
replay via `getReader()`.

- `HttpServletRequestBodyCachingFilter` now wraps requests in its own
  `HttpServletRequestWithCachedBody`, which buffers the body once and serves a fresh stream on
  every `getInputStream()`/`getReader()` call.
- Form-urlencoded and multipart requests are passed through unbuffered, because the container
  parses those bodies itself for `getParameter(...)` (e.g. `POST /fake-jwt/token`).
- Verified by `HttpServletRequestBodyCachingFilterUnitTest` and
  `RbacSubjectControllerAcceptanceTest.AuditLogOfMutatingRequests.createSubjectRequestIsAuditLoggedIncludingRequestBody`.

### masking of sensitive request body properties

A consequence of the fix above: request bodies now actually reach the audit-log, so `Context.toCurl`
masks the values of sensitive JSON body properties as `<masked>` before logging. Property names are
matched (recursively, case-insensitively) against `Context.BODY_PROPERTIES_TO_MASK`: names ending
with `password` or `totpKey`, or starting or ending with `secret` — e.g. the write-only
hosting-asset config properties. Non-JSON bodies are logged unchanged. Verified by the
`ContextUnitTest` masking cases.

## Non-Goals

This PR deliberately does **not**:

- synchronize user-accounts (which require an associated person);
- synchronize or persist group membership (membership is still derived from the JWT `groups` claim
  at request time, as in PR#236);
- implement the external Keycloak sync program itself (only the backend APIs it calls);
- support batch/multi-subject synchronization (one subject per call is sufficient for now);
- provide an automated purge *job* (on-demand purge via `DELETE …?purge=true` **is** in scope, but
  deciding which long-deactivated subjects to purge and when is not);

## Prerequisite PRs

- PR#234: Prefixed user subject names instead of email addresses.
- PR#236: Realm-prefix-based user and group subject visibility.

## Follow-up PRs

1. Synchronize user-accounts (subject plus associated person) from Keycloak.
2. Synchronize group membership from Keycloak instead of deriving it from the JWT `groups` claim.
3. Automated purge job for long-deactivated subjects (the on-demand `?purge=true` operation lands in
   this PR; only the scheduling/selection policy remains).
4. Design cross-realm grants and the required trust relationship between realms.
