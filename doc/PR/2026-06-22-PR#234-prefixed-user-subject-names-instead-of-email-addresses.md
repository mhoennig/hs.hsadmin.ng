# PR#234: Taiga#471: Prefixed user subject names instead of email addresses

## Related Links (Hostsharing-internal)

- Taiga-Story: https://plan.hostsharing.net/project/admin-hsadmin/us/471
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/234

## The Problem

`rbac.subject.name` is currently a freely chosen, unique name of up to 63 characters.
Historically, the user subjects look like email addresses, for example:

```text
superuser-alex@hostsharing.net
selfregistered-user-drew@hostsharing.org
mass-account-0001@example.com
```

These values are not actual email addresses but login names, i.e. user subjects to which roles can be granted.
But the test data currently mixes that up with real email addresses, e.g. in contact data, hosting email assets, alias targets, and integration data.

This conflation causes several problems:

- The projected subject names in the Hostsharing Assembly Keycloak realm will be prefixed usernames like 'xyz-some_user', and test data should match the real data in the structure to avoid confusion.
- Distinguishing a ReBAC subject from a business email address is only possible from context.
- The realm a subject belongs to cannot be reliably derived from its name.
- The planned visibility of subjects within a partner realm requires a stable naming convention or an organization property in JWT, sync-data, and table `rbac.subjects`.
- Some test-data generators reuse the same value as both a real email address and an `rbac.subject.name`.

## The Solution

User subjects adopt a uniform naming scheme:

```text
<realm-prefix>-<customer-defined-local-part>
```

Example:

```text
superuser-alex@hostsharing.net  ->  hsh-superuser_alex
```

The realm prefix identifies the partner realm and corresponds to the debitor prefix or a unique prefix derived from it.

Proposed pattern for `USER` subjects:

```text
^[a-z]{3,5}-[a-z0-9._]+$
```

These look a bit like webspace-prefixes, but even though they often might be the same, they are generally independent.

Rules:

- `realm-prefix`: 3 to 5 lowercase characters, e.g. `hsh` or `abcde`
- `customer-defined-local-part`: lowercase letters, digits, dots, and underscores
- `-`: separator between realm prefix and local part

## Where Subject Names Come From and Are Used

Usernames are sent from Keycloak (Assembly Realm) via an API as part of a synchronization program
to HSAdmin NG which stores them in the table column `rbac.subject.name`.

For JWTs, the UUID is read from the `sub` claim with a fallback to `preferred_username`.
The resolved value is passed to `base.defineContext(...)`.

On the database side, `rbac.determineCurrentSubjectUuid(currentSubject)` resolves the name to a UUID via `rbac.subject.name`.

A value is likely a Subject-name if it is:

- passed to `base.defineContext(...)` or `context.define(...)`
- used in test helpers such as `context(...)` or `bearer(...)`
- passed to `rbac.create_subject(...)`
- stored in or compared against the table column `rbac.subject.name`
- appearing after `user:` in expected grant output
- configured as `hsadminng.superuser`

A value is likely a real email address if it is:

- stored in the table column `hs_office.contact.emailAddresses`
- used as the `emailAddress` query parameter for contacts
- associated with hosting assets of type `EMAIL_ADDRESS` or `EMAIL_ALIAS`
- appearing in alias targets
- used in email-address or email-hosting validations

## Excluded from Scope

- real email addresses in `hs_office.contact.emailAddresses`
- email addresses and alias targets in hosting assets
- email fields in integrations such as Znuny, Kimai, or MLMMJ
- arbitrary business email addresses in tests, as long as they are not used as `rbac.subject.name`
- subject visibility rules

## Prerequisite PRs

- PR#223: Support user groups as RBAC subject (introduces `USER` / `GROUP` subject types).

## Implemented Changes

### Database Constraint

- [x] Add a helper function `rbac.is_valid_user_subject_name(subjectName varchar)`
  (see repeatable changeset `rbac-base-SUBJECT-USER-NAME-VALIDATION` in `1050-rbac-base.sql`).
- [x] Add a helper function `rbac.is_valid_group_subject_name(subjectName varchar)`
  (see repeatable changeset `rbac-base-SUBJECT-GROUP-NAME-VALIDATION` in `1050-rbac-base.sql`).
- [x] Add a `CHECK` constraint to `rbac.subject` enforcing the type-specific pattern for both supported subject types:
  realm-prefixed names for `USER` subjects and `/...` names for `GROUP` subjects
  (see changeset `rbac-subject-CHECK-SUBJECT-NAME` in `1085-rbac-user-subject-names.sql`).

### Seed-Subject Renaming

- [x] Rename global seed subjects in `1080-rbac-global.sql`:

  | Old name                                   | New name                  |
  |--------------------------------------------|---------------------------|
  | `superuser-alex@hostsharing.net`           | `hsh-alex_superuser`      |
  | `superuser-fran@hostsharing.net`           | `hsh-fran_superuser`      |
  | `selfregistered-user-drew@hostsharing.org` | `tst-drew_selfregistered` |
  | `selfregistered-test-user@hostsharing.org` | `tst-rene_selfregistered` |
  | `import-superuser@hostsharing.net`         | `hsh-import_superuser`    |

- [x] Apply the same renaming consistently in Java tests, scenario tests, and documentation.
- [x] Rename the superuser/JWT defaults in the environment and test configuration to the new format:
  - `HSADMINNG_SUPERUSER` and `HSADMINNG_JWT_USERNAME` in `.tc-environment`.
  - the `hsadminng.superuser` property default in `ImportHostingAssets` and `LiquibaseCompatibilityIntegrationTest`.

### Test-Data Generator Refactoring

- [x] Separate subject-name variables from email-address variables wherever a single value is used for both purposes.
  Affected generators include `hs_office.partner_create_mass_bundle_test_data` and `hs_accounts.account_create_mass_test_data` in `9520-hs-mass-test-data-generators.sql`, as well as `9519-hs-accounts-test-data.sql`.
  Where a debitor prefix is already computed for mass data, the subject prefix should be derived from it; otherwise a stable test prefix such as `tst` is sufficient.

  Example refactoring:

  ```sql
  -- before
  emailAddr = 'contact-admin@' || base.cleanIdentifier(contCaption) || '.example.com';
  perform rbac.create_subject(emailAddr);

  -- after
  subjectName = 'tst-contact_admin_' || base.cleanIdentifier(contCaption);
  emailAddr   = 'contact-admin@' || base.cleanIdentifier(contCaption) || '.example.com';
  perform rbac.create_subject(subjectName);
  ```

The same separation applies to `person`, `bankaccount`, and `account` test-data generators.

### OpenAPI and Java Validation

- [x] Model `RbacSubjectInsert` as `anyOf` alternatives for `RbacUserSubjectInsert` and `RbacGroupSubjectInsert`,
  each with its own `type` enum and `name` pattern.
- [x] Add DB-backed validators in `RbacSubjectController` that reject invalid `USER` or `GROUP` subject names
  with `400 Bad Request` before the database constraint is hit.
- [x] Translate invalid-subject-name responses via the existing message bundle.
- [x] Adapt `DataIntegrityViolationException` handling to honor translated DB error status markers such as
  `ERROR: [400]`, so subject-name check-constraint violations return `400 Bad Request` instead of the
  generic `409 Conflict` fallback.
- [x] Add tests for valid and invalid `USER` and `GROUP` subject names.
- [x] Add a drift guard that compares the OpenAPI patterns, generated Bean Validation patterns, and SQL function patterns.

<!-- disable:fixmes -->
### Review and FIXME Tooling

- [x] Document the in-code `FIXME.review` convention for code-review findings in `AGENTS.md`.
      That tool can be used to determine if there are any actionable FIXMEs left in the branch.
- [x] Implement a `fixmes` CLI-tool to list all actionable FIXMEs, 
      but not those with `<!-- disable:fixme(s) -->` / `<!-- enable:fixme(s) -->`
      to be able to use FIXME in a non-actionable manner for docucumentation purposes.

<!-- enable:fixmes -->

### Liquibase Changeset Formatting

- [x] Move `validCheckSum:ANY` off the `--changeset` line into a separate `--validCheckSum: ANY` line in all
  affected repeatable changesets (across `0-base/*` and `1-rbac/*` changelogs, e.g. `020-audit-log.sql`,
  `1050-rbac-base.sql`, `1054-rbac-context.sql`, `1055-rbac-views.sql`, `1058-rbac-generators.sql`,
  `1059-rbac-statistics.sql`, and others touched by this PR). <br/>
  REASON: The other syntax does not properly work if not accompanied by `runOnChange:true`, as for test-data changes.
- [x] Document the corresponding changeset-formatting rules in `AGENTS.md`
  (checksum allowance on its own `--validCheckSum: ANY` line; keep `runOnChange:true`, `context:...`,
  and `endDelimiter:--//` on the `--changeset` line).

### Migration-Test Adjustments

- [x] The method `CsvDataImport.makeSureThatTheImportAdminUserExists()` is not necessary anymore
      it's in the test-data by now, and the test-data migration takes care for converting it to the new pattern.

### Migration Compatibility

- [x] Run migration tests after all data changes:
  `LiquibaseCompatibilityIntegrationTest` and `ImportHostingAssets` via `. .tc-environment; ./gradlew migrationTest`.

## Impact on Keycloak

For real JWTs, Keycloak must deliver the new subject name pattern.
If HSAdmin NG continues to use `preferred_username` as the subject name, `preferred_username` must conform to the new pattern.

The stable UUID should continue to come from the Keycloak `sub` claim and be synchronized as `rbac.subject.uuid`.
The new name is therefore not the technical identity but the readable, realm-scoped subject name.

## Impact on Realm Visibility

The pattern enables simple prefix derivation:

```sql
split_part(subjectName, '-', 1)
```

This allows future rules to identify subjects belonging to the same partner realm, for example:

- showing all user subjects within a user's own realm
- showing matching group subjects, provided the group naming scheme is defined compatibly
- plausibility checks for grants within a realm

Cross-realm grants remain a separate topic and must not be permitted by name similarity alone; they still require an explicit trust or release rule.

## Open Requirements and Design Issues

- How existing production subjects get migrated: a `RENAME-SEED-SUBJECTS` changeset in `1080-rbac-global.sql`
  renames all known seed subjects by name. A second changeset in `1085-rbac-user-subject-names.sql`
  renames all test-data subjects by pattern. For subjects beyond those (real production accounts),
  a separate migration step outside this PR is needed once Keycloak delivers the new names.

## Non-Goals

- Do not change the ReBAC graph itself.
- Do not change business email-address fields.

## Follow-up PRs

1. Extend subject visibility so users can see only user and group subjects within their own realm (enabled by the realm-prefix derivation above).
2. Synchronize users and groups from Keycloak to HSAdmin NG using the new subject-name pattern.
