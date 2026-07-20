# PR#244: API-Key Authentication for Global-Admin Access, Bypassing Keycloak

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/480
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/244

## The Problem

Every REST request needed an OIDC JWT from the Hostsharing Keycloak.

Technical clients (deployment, sync) thus always needed a Keycloak login, there was no access when Keycloak is down,
and no long-lived machine-to-machine credentials.

And bootstrapping was impossible because there was no subject synced between Keyloak and backend,
thus bootstrapping was only possible by tinkering with the database.

Especially the Keycloak-Sync needs credentials to perform the sync following the least privileges principle.

## The Solution

We introduce API-keys right at the backend level, bypassing Keycloak.

## The Requirements

### Feature: API-keys as an alternative authentication, in parallel to Keycloak OIDC JWTs

#### Background

We introduce `API_KEY` as a third `rbac.SubjectType` (besides `USER`/`GROUP`); the clear-text key
`hsak_<subject-name>.<64 hex>` is returned only once, only its SHA-256 hash is stored in the database, and an
optional `expiresAt` binds its lifetime.

Authorization stays role-based — global-admin access is the global ADMIN role, granted via the grants API. Subject-type model,
name grammar and key format: see [RFC#0002](../rfc/2026-07-14-RFC%230002-api-keys.md) (Zielbild, Namensraum).

#### Scenario: Bootstrapping the provisioning global-admin API-key from the configuration

Starting from a legacy dump there is no subject at all, so nobody could create the first key.

- **Given** the SHA-256 hash of a key in `HSADMINNG_PROVISIONING_API_KEY_SHA256` (the clear-text
  key exists only at the client)
- **When** the application starts
- **Then** the API_KEY subject `hsadminng.provisioning.key` is provisioned with the global ADMIN role,
  but only if no key is stored for it yet: a stored key always wins, so further starts never
  change it (a differing configured hash only logs a warning)
  - **and** requests with just the `Hostsharing-Api-Key` header act as that subject with
    global-admin power, bypassing Keycloak
  - **and** further keys can be created at runtime with an existing key (`POST
    /api/rbac/subjects` + `POST /api/rbac/grants`), as can any global admin.

#### Scenario: Revoking an API-key

- **Given** an existing API_KEY subject
- **When** a global admin deletes it (`DELETE /api/rbac/subjects/{uuid}`), repeating the
  subject's `name` and `type` as safeguard query parameters, which match the subject
- **Then** the subject, its grants, and its stored key hash are physically deleted
  - **and** the key is rejected `401` from then on, immediately and permanently — there is no
    soft-delete via `DELETE`, and `PUT` (the only deactivation path) rejects API_KEY subjects,
    so deactivated API-keys cannot exist.

#### Scenario: An attempt to revoke an API-key without matching safeguard values fails

- **Given** an existing API_KEY subject
- **When** a global admin sends `DELETE /api/rbac/subjects/{uuid}` with a mismatching or
  missing `name`/`type` query parameter
- **Then** the request is rejected `400 Bad Request`
  - **and** the subject is untouched and its key keeps authenticating.

### Feature: API-keys restricted to named endpoint-scopes

#### Background

Named endpoint-scopes are an optional fence on top of roles: a scoped key may only call endpoints
matched by one of its scopes, else `403` (fail-closed); no scopes = unrestricted. The two scopes
here: `rbac.subjects:sync` (GET + `PUT /api/rbac/subjects[/{uuid}]`, deliberately not `POST`/`DELETE`,
for the Keycloak sync) and `*:read` (`GET /api/**`, read-only). Motivation and the scope-mapping
design: see RFC#0002 (Endpoint-Scopes).

#### Scenario: An endpoint-scoped API-key for the Keycloak subject synchronization

- **Given** an API_KEY subject with `"scopes": ["rbac.subjects:sync"]` and the global ADMIN role
- **When** the key calls `GET /api/rbac/subjects`, `GET /api/rbac/subjects/{uuid}` or `PUT
  /api/rbac/subjects/{uuid}`
- **Then** it syncs the subjects of ALL realms via its global-admin visibility
  - **and** any other endpoint (e.g. `POST /api/rbac/subjects` or a business API) responds `403`
  - **but** non-`/api/` paths, `/api/ping` and `GET /api/rbac/context` remain unaffected.

#### Scenario: An API-key inspects its own properties

- **Given** any API_KEY subject, even a scoped one
- **When** it calls `GET /api/rbac/context`
- **Then** the response contains its subject (UUID, name, type) plus the `apiKey` property
  with the key's scopes and expiry.

#### Scenario: The subject synchronization cannot touch API_KEY subjects

API_KEY subjects exist only in hsadmin-ng. A state-replicating sync treating them as "removed
from Keycloak" would deactivate ALL keys, including its own and `hsadminng.provisioning.key` (lockout).

- **Given** the Keycloak sync, deactivating removed subjects via `PUT deactivated:true`
- **When** it sends `PUT /api/rbac/subjects/{uuid}` with `type: API_KEY`
- **Then** it is rejected `400` — API_KEY subjects cannot be created, renamed, deactivated, or
  reactivated via `PUT`
  - **and** since `DELETE` is not in `rbac.subjects:sync`, a sync key cannot touch keys at all.

## The Implementation Details

- New `rbac.SubjectType` value `API_KEY` (changesets in `1050-rbac-base.sql`; name check
  constraint in `1085-rbac-user-subject-names.sql`).
- API_KEY names follow `^[a-z0-9]+(\.[a-z0-9]+)*$` (OpenAPI pattern + DB
  `rbac.is_valid_api_key_subject_name`); usernames with explicit organization must contain
  `-` or `@`, keeping the namespaces disjoint (RFC#0002). Bootstrap subject renamed
  `initial_api_key` → `hsadminng.provisioning.key`; the reserved `hsadminng.` prefix keeps
  the flat API-key namespace free for customer-created keys without conflicts.
- `ProvisioningApiKeyBootstrap` (ApplicationRunner) provisions `hsadminng.provisioning.key` with the global
  ADMIN role from `HSADMINNG_PROVISIONING_API_KEY_SHA256` on start, idempotently (stored key wins).
- A one-time Liquibase changeset (`rbac-api-key-RENAME-PROVISIONING-SUBJECT` in
  `1086-rbac-api-key.sql`) renames an already-provisioned subject from an earlier name to
  `hsadminng.provisioning.key`, so no manual DB step is needed and no second admin subject is created.
- New table `rbac.api_key` (`1086-rbac-api-key.sql`) stores the SHA-256 hash keyed by subject
  UUID (`on delete cascade`) plus optional `expires_at`. `rbac.create_api_key(...)` re-checks
  global-admin and the API_KEY type at the DB level.
- New table `rbac.api_key_scope` (same changelog) stores scope names, one row per key+scope,
  cascading; no rows = unrestricted.
- `POST /api/rbac/subjects` accepts the `RbacApiKeySubjectInsert` variant with an optional
  `scopes` array (enum `ApiKeyScope`, unknown values `400`); it generates the key, stores hash
  and scopes, and returns the clear-text key once in the dedicated `RbacSubjectCreated` schema.
- `ApiKeyAuthenticationFilter` (before `BearerTokenAuthenticationFilter`) hashes the
  `Hostsharing-Api-Key` header, looks up the active API_KEY subject, and synthesizes an
  in-memory `JwtAuthenticationToken` (subject UUID as `sub`, `token_type: api-key`, scopes in
  the `scope` claim, expiry in `api_key_expires_at`) — no issuer/signature/decoder involved.
  Unknown keys `401`; requests with both `Authorization` and `Hostsharing-Api-Key` are
  rejected `401` ("multiple authentication methods are not allowed", no precedence).
- `GET /api/rbac/context` additionally returns the used key's `apiKey` (`scopes`, `expiresAt`);
  null for JWT authentications.
- `PUT /api/rbac/subjects/{uuid}` uses schema `RbacSubjectUpsert` (USER/GROUP only): `type:
  API_KEY` is rejected `400`; the optional `deactivated` property is the only way to deactivate
  a subject (soft-delete: row and grants kept but hidden, original timestamp preserved). In the
  repeatable `rbac.upsert_subject` changeset, incl. creating a subject already deactivated.
- `DELETE /api/rbac/subjects/{uuid}` always deletes physically (former soft-delete default and
  `purge` param gone); the FK cascade drops the key hash, revoking the key. `name` and `type`
  must be repeated as query parameters and match the subject (`400` else); unknown UUID `204`
  (idempotent). This removes the deactivated-key state that caused the misleading
  "already provisioned" bootstrap log; recovery after a deletion is just a restart.
- `GET /api/rbac/scopes` (new `RbacScopeController`) lists the scopes and their allowed
  endpoints from the `ApiKeyScope` enum.
- `ApiKeyScope` is the single source of truth mapping each scope to its method+path allowlist
  (`PathPatternRequestMatcher`); a unit test keeps it in sync with the generated OpenAPI enum.
- `StrictBodyConverter` renders invalid enum values readably, e.g. `scopes[0] 'unknown:scope'
  is not valid, valid values are: [rbac.subjects:sync, *:read]`.
- `ApiKeyScopeEnforcementFilter` (after `BearerTokenAuthenticationFilter`) rejects out-of-scope
  requests `403`, fail-closed, for `/api/` except `/api/ping` and `GET /api/rbac/context`;
  JWTs and unscoped keys pass through.
- The `Hostsharing-Api-Key` header is excluded from the audit-log; the header comparison in
  `Context.toCurl` is now case-insensitive.
- `ApiKeyScenarioTests` (order 96xx) document the whole workflow: create the key, grant the
  global ADMIN role, use it on a business API and on the bootstrap; the scoped-key cases
  (sync, `403` for `POST`/business, `*:read`), self-inspection, revocation, and the
  safeguard-failure. Framework got `FakeLoginUser.withApiKey(...)`; reports mask the key as
  `$HSADMINNG_API_KEY`.
- Tooling: README HOWTO "Authenticate with an API-Key"; `tools/http` implicitly adds the
  `Hostsharing-Api-Key` header from `HSADMINNG_API_KEY` (masked in its echo); `.aliases`
  `APIKEY`/`LOGOUT` (GPG-encrypted `.apikeys.gpg`, `--transient` for no storage);
  `tools/remote provisioning-api-key` performs the bootstrap on the deployed backend.

## Open Issues

- The external Keycloak sync must switch to `PUT deactivated:true`, 
  `DELETE` must not be used by the Keycloak sync for this purpose anymore.

## Non-Goals / Follow-Ups

- **Provisioning key rotation**:
  If you revoke `hsadminng.provisioning.key`, the revocation is permanent only if you also rotate
  or remove `HSADMINNG_PROVISIONING_API_KEY_SHA256`. Add a runbook and a clear bootstrap
  diagnostic message in a follow-up PR (RFC#0002, "Recovery des Provisioning-API-Keys").

- **Provisioning key expiry**:
  The provisioning key never expires (`expires_at` stays null); it is revoked only by rotating
  `HSADMINNG_PROVISIONING_API_KEY_SHA256` or deleting the subject. A configurable expiry for it
  is out of scope.

- **Additional endpoint scopes**:
  Add one `ApiKeyScope` constant and one OpenAPI enum value for each new endpoint scope.

- **API-key creator**:
  At present, you can identify the creator only from the audit journal
  (`base.tx_journal_v`, `currentSubject` of the `rbac.subject` `INSERT`). It is still open whether
  to expose this information through an API endpoint or a `created_by` column. See RFC#0002,
  "Nicht-Ziele / Follow-Ups".

- **Mandatory JWT audience (`aud`) validation**:
  The system always validates `iss`, because `issuer-uri` is mandatory. The system validates `aud`
  only when `HSADMINNG_JWT_AUDIENCE` is set. Current HS Keycloak tokens do not contain an `aud`
  claim. In a follow-up task, add a Keycloak audience mapper and then set
  `HSADMINNG_JWT_AUDIENCE` (recommended value: `hsadmin-ng-api`). Use one dedicated `aud` value
  for each service. Do not use one token with multiple audiences. If such a token is exposed at
  the weakest service, an attacker could also access the admin backend.

- **Other non-goals**:
  API-key rotation endpoints, realm-scoped keys, and subject-name uniqueness per
  `(type, name)` are out of scope. See RFC#0002, "Nicht-Ziele / Follow-Ups".
