# PR#242: Create Accounts by Subject-UUID, also as Global-Admin without an own Account

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/242

## The Problem

`POST /api/hs/accounts/accounts` had some shortcomings:

1. The related RBAC subject could only be specified by `subjectName`, and a **new** subject was
   always created. Since PR#238, USER subjects are synchronized from Keycloak and identified by
   their **UUID** (the same UUID as in Keycloak) — the UUID is now the required reference,
   creating subjects just by name is not supported anymore.
2. The endpoint resolved the **acting** global-admin's own account, even though it never used it —
   a global-admin subject **without** an own account (e.g. the Keycloak sync program or a
   freshly bootstrapped admin) got a `500 Internal Server Error` instead of being able to
   create accounts.
3. It would be useful for bootstrapping the initial admin accounts if the endpoint also
   created the user-subject, if it does not yet exist.
4. Also `GET /api/hs/accounts/current` returned a `500 Internal Server Error` for subjects
   without an account, instead of just omitting the person data.

## The Requirements

### Feature: Create accounts for subjects identified by their Keycloak-UUID

#### Background

- An account combines an RBAC USER subject with a natural person and thus grants access to data
  in hsadmin-NG.
- Since PR#238, USER subjects are synchronized from Keycloak; a subject is identified by its
  **UUID** (the same UUID Keycloak uses), while its name is mutable.
- The person of the new account is either referenced by its UUID (property `person.uuid`),
  or created from an inline person object (property `person`) if it does not exist yet
  in the backend.
- The subject of the new account is modelled in the same (parallel) structure:
  it is either referenced by its UUID (property `subject.uuid`),
  or created from an inline subject object (property `subject`, with required `uuid` and `name`
  and optional `type`, where `USER` is the default and only allowed value)
  if it does not exist yet in the backend.
  Creating a subject just by its name (with a generated UUID) is not supported anymore.
- Currently, only global-admins can create new accounts; the acting global-admin subject
  does **not** need to have an own account itself.

#### Scenario#242.01: A global-admin without an own account can create accounts
<a id="scenario-242.01"></a>

so that e.g. a freshly bootstrapped admin or a technical admin subject (like the Keycloak sync
program) can create accounts without needing an account (and thus a person) itself.

- **Given** a USER subject freshly synchronized from Keycloak (`PUT /api/rbac/subjects/{uuid}`)
  - **and** the global ADMIN role got granted to that subject (`POST /api/rbac/grants`)
  - **and** that subject has no own account, verified via `GET /api/hs/accounts/current`
    returning the subject without any person data (formerly a `500 Internal Server Error`)
- **When** that global-admin creates an account, e.g. as in [Scenario#242.02](#scenario-242.02)
- **Then** the account is created
  - **and** the response returns `201 Created` (formerly `500 Internal Server Error`)

##### Verified by

- `AccountScenarioTests.shouldCreateGlobalAdminSubjectWithoutAnOwnAccount`

#### Scenario#242.02: A single request can create the person, the USER subject, and the account
<a id="scenario-242.02"></a>

so that e.g. initial admin accounts can be bootstrapped with a single call, without any
preexisting person or subject in the backend.

- **Given** the account-less global-admin from [Scenario#242.01](#scenario-242.01)
  - **and** a subject UUID `US-UUID-1` (e.g. assigned by Keycloak) and its name for which no subject exists yet
    in the backend
  - **and** no matching person exists yet
  - **and** the person details (name etc.) are known
- **When** the global-admin creates an account with an inline `person` object containing the person details
  and an inline `subject` object with `uuid` = `US-UUID-1` and a `name`, in a single `POST` request
- **Then** the person is created
  - **and** a new USER subject with UUID `US-UUID-1` and the given name is created
  - **and** the account is created for that subject and person
  - **and** the response returns `201 Created`
  - **and** the user-subject can SELECT the newly created person

##### Verified by

- `AccountScenarioTests.shouldCreatePersonAndSubjectAndAccountWithASingleRequest`

#### Scenario#242.03: A missing USER subject is created along with the account, from an inline subject object
<a id="scenario-242.03"></a>

so that an account with a Keycloak-assigned subject UUID can be created in a single call,
even before the subject got synchronized to the backend.

- **Given** a global-admin
  - **and** a subject UUID `US-UUID-1` (e.g. assigned by Keycloak) and its name for which no subject exists yet
    in the backend
  - **and** the person with UUID `P-UUID-1` exists
  - **and** that person has at least one relation and at least one membership
- **When** the global-admin creates an account with an inline `subject` object
  with `uuid` = `US-UUID-1` and a `name`
  - **and** the referenced person for the new account (`person.uuid`) has UUID `P-UUID-1`
- **Then** a new USER subject with UUID `US-UUID-1` and the given name is created
  - **and** the account is created for that subject
  - **and** the response returns `201 Created`
  - **and** the user-subject can SELECT the person `GET /api/hs/office/persons/{P-UUID-1}`
  - **and** the user-subject can SELECT the relations and memberships of that person

##### Verified by

- `AccountScenarioTests.shouldCreateAccountAndUserSubjectFromGivenSubjectUuidAndName`

#### Scenario#242.04: A missing person is created along with the account, from inline person details
<a id="scenario-242.04"></a>

so that accounts can be created for persons who are not persisted in the backend yet.

- **Given** a global-admin
  - **and** a USER subject with UUID `US-UUID-1`, e.g. previously synchronized from Keycloak
  - **and** that subject does not have an account yet
  - **and** no matching person exists yet
- **When** the global-admin creates an account with `subject.uuid` = `US-UUID-1`
  - **and** an inline `person` object instead of a person UUID
- **Then** the person is created
  - **and** the account is created for the existing subject `US-UUID-1` and the new person
  - **and** no additional subject is created
  - **and** the response returns `201 Created`
  - **and** the user-subject can SELECT the newly created person

##### Verified by

- `AccountScenarioTests.shouldCreateAccountWithNewPersonForPreexistingUserSubject`

#### Scenario#242.05: A global-admin creates an account for a preexisting USER subject and a preexisting person
<a id="scenario-242.05"></a>

so that accounts can be created for subjects which were already synchronized from Keycloak,
referencing an already registered person.

- **Given** a global-admin
  - **and** a USER subject with UUID `US-UUID-1`, e.g. previously synchronized from Keycloak
  - **and** that subject does not have an account yet
  - **and** the person with UUID `P-UUID-1` exists
- **When** the global-admin creates an account with `subject.uuid` = `US-UUID-1`
  - **and** the referenced person for the new account (`person.uuid`) has UUID `P-UUID-1`
- **Then** the account is created for the existing subject `US-UUID-1`
  - **and** no additional subject is created
  - **and** the response returns `201 Created`
  - **and** the user-subject can SELECT the person `GET /api/hs/office/persons/{P-UUID-1}`

##### Verified by

- `AccountScenarioTests.shouldCreateAccountForPreexistingUserSubjectViaSubjectUuid`
- `HsAccountControllerRestTest.postNewAccountReferencingAnExistingUserSubjectUsesThatSubject`
  (asserts that no additional subject is created)

## Background: the unused `LoginContext.account`

`HsAccountController.postNewAccount` constructed a private inner `LoginContext`, which eagerly
resolved the acting subject's own account and threw the `500` if there was none. That `account`
field was assigned but never read anywhere — only `isGlobalAdmin` and `subjectUuid` were used.
The whole inner class was therefore removed and replaced by direct `context.isGlobalAdmin()` /
`context.fetchCurrentSubjectUuid()` calls.

## The Solution

- `AccountInsert` (OpenAPI) models the subject like the person: `subject.uuid` references
  an existing USER subject, an inline `subject` object (new schema `AccountSubjectInsert`,
  `uuid` and `name` required, `type` only allows `USER`) creates a new one.
  The flat `subjectUuid`/`subjectName` properties are gone.
- The `Account` response returns the subject as an `RbacSubject` JSON object instead of just
  a flat `subjectName`; its UUID deliberately repeats the account UUID.
- `HsAccountController.fetchOrCreateSubject` requires exactly one of `subject`/`subject.uuid`:
  a referenced subject must exist (visible, not deactivated), be a USER subject, and not yet
  have an account; an inline subject must not exist yet and is created with the given
  UUID and name. Each invalid subject reference is rejected with `400 Bad Request`
  without creating an account or subject, verified by one
  `HsAccountControllerRestTest.postNewAccount…IsRejected` test per rejection case.
- The acting admin's own account is not resolved anymore (see Background above), which fixes
  the `500` for global-admins without an own account.
- `GET /api/hs/accounts/current` returns the subject without person data instead of a `500`
  if the current subject has no account.

## Additional Changes

- The audit-log task text for activating the account subject changed from
  `activate newly created self-service subject` to `activate the account subject`,
  because that subject is not necessarily newly created anymore.
- `ViewRbacSubjectsAssociatedWithSamePerson` and `ViewRbacSubjectsNarrowedByFilters`
  now create their same-person accounts with the subject UUID
  (resolves the `TODO.impl[Taiga#471]` comments there).
- The acceptance test for account-less global-admins was removed; the bootstrap scenarios
  cover that case purely API-driven, including the role grant via `POST /api/rbac/grants`.
- A curl wrapper which is compatible to the HTTP calls in the scenario-test reports is defined in `.aliases`.
- Scenario reports render request bodies as a shell here-document (`--data-binary @- <<EOF`),
  so reported requests can be pasted into a shell and literal values replaced by shell
  variables; `$`, `` ` `` and `\` get escaped, and the `// alias` comments stay in the body
  (the API tolerates JSON comments). The request URL is prefixed with `$HSADMINNG_API_BASE_URL`
  and the auth header is rendered as `-H "Authorization: Bearer $HSADMINNG_JWT_BEARER"` — both
  double-quoted, expanded from the user's environment after copy+paste. The fake JWT
  claims follow as `` `# ...` `` pseudo-arguments, which the shell drops on execution.

## Non-Goals

- Subjects are deliberately not referencable via their name, only via their UUID,
  because the UUID is the stable identity shared with Keycloak and the name is mutable.
- Account creation is still global-admin-only; automatic account creation upon Keycloak
  synchronization remains a follow-up (see PR#238 follow-ups).
