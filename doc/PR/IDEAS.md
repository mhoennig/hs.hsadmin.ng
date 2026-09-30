# Ideas and Free-Floating TODOs

This file collects ideas and TODOs which hang on no specific code location
and belong to no open PR yet.
Several related entries can later grow into a PR of their own;
a single entry may join a PR as an "Additional Change"
when it roughly fits that PR's topic or the code it touches.

Conventions:

- One entry per bullet in the format `- [ ] TODO.<area>: <idea>`,
  with enough context to understand it later without the original conversation.
- Whoever takes an entry into a PR moves it into that PR document
  and deletes it here; nothing gets checked off in this file.

## Entries

- [ ] TODO.test: Add scenario-test coverage for the booking and hosting endpoints.
      The rule "each API endpoint needs at least one scenario test with a realistic use case"
      is unfulfilled for the whole hs/booking and hs/hosting API.
      Only the list endpoints for booking projects and hosting assets show up at all,
      as a side aspect of the account scenarios.
      Missing are create/patch/delete for booking projects, booking items and hosting assets,
      as well as the type-metadata endpoints and the hosting-asset list filters.
- [ ] TODO.test: Consider a check against boolean-heavy signatures and types.
      Neither the PMD ratchet (`config/pmd/complexity.xml`) nor the `ArchitectureTest` notices
      a record full of booleans or a parameter list with several booleans today.
      Candidates: PMD `ExcessiveParameterList` in ratchet style, a custom XPath rule,
      or an ArchUnit rule against more than two `boolean` parameters per method.
      The underlying design question — flags instead of enums or dedicated types — stays with the review.

- [ ] TODO.feat: Add `GET /api/hs/booking/item-types` and `GET /api/hs/booking/item-types/{itemType}`
      analogous to `/api/hs/hosting/asset-types[/{assetType}]`,
      so an adaptive UI can query the booking-item types and their resources validation
      (CPU, RAM, SSD …) instead of hardcoding them.
      The server side already has the data in the validators
      under `hs/booking/item/validators/` (`HsBookingItemEntityValidatorRegistry`),
      built on the same `HsEntityValidator` infrastructure as the hosting-asset validators;
      needs OpenAPI YAML plus a controller method reading the registry properties.

- [ ] TODO.doc: Annotate the scenario tests of already merged PRs with `@Scenario`
      and generate the `Verified by` sections of their PR documents with `tools/generate-verified-by`.
      PR#304 introduced both only for new PRs; without an annotation the tool reports nothing.
- [ ] TODO.refa: Replace the `@Order` numbers of the scenario tests by a thematically ordered,
      alphanumeric key with separators, e.g. `office.bankaccount.complete-bic`.
      The number orders the scenario reports today, but it says nothing about the subject
      and has to be kept free by hand.
      A combined annotation could carry both: the subject key, which also orders and names
      the report index, and the PR-based Gherkin scenario ids of `@Scenario`,
      which tie a test to the PR documents that specify it.
      `@Scenario` (PR#304) is the first step; it already separates the two concerns,
      because the test no longer needs its report name to be found by a PR document.

- [ ] TODO.refa: Split the overloaded `test` profile so that the test configuration can inherit
      the defaults of `src/main/resources/application.yml` instead of shadowing them.
      `test` currently means both "test infrastructure" (Testcontainers datasource, Liquibase contexts,
      actuator exposure) and "replace the production security", because `WebSecurityConfig`
      carries `@Profile("!test")`.
      The integration tests which deliberately keep the production security therefore must not activate
      `test` and would lose the infrastructure settings with it,
      which is why `src/test/resources/application.yml` still shadows the production file
      and repeats every shared value.
      Rename the security switch, for example to `@Profile("!mock-security")`,
      then the infrastructure settings fit the ordinary Spring Boot pattern:
      an `application-test.yml` which every test activates, with no shadowing and no repetition.

- [ ] TODO.refa: Replace the negative Liquibase context `without-test-data` by a positive `test-data`.
      Today a start without `without-test-data` inserts test data irreversibly and unnoticed,
      e.g. into production; with `test-data` the default would be an empty database.
      The `bootRun` calls in `.aliases`, `README.md` and `tools/smoke-test` already pass `test-data`,
      but no changeset reads it yet.
      Invert about 55 `!without-test-data` contexts in the changelogs, including the `2-rbactest` schema,
      `5056-…` becomes `test-data or demo-bankcodes`.
      Add `test-data` to the test `contexts`, drop `without-test-data` from `ImportHostingAssets`,
      `DemoDataScenarioTest` and `tools/demo-data`, and coordinate the deployment profiles with Ops.
      `migrationTest` must prove that the changed contexts keep the released checksums.

- [ ] TODO.feat: Import the Bundesbank bank-code file from the NExt platform instead of the public, non-binding download.
      NExt offers the binding version via REST with OAuth2 client credentials,
      but needs two registrations: Hostsharing as a legal entity via routing@bundesbank.de
      and a technical client via next@bundesbank.de; the API documentation is available on request only.
      `HsOfficeBankDirectoryImporter` (PR#307) could switch the source via a property like
      `hsadminng.bank-directory.import.source=public|next` and keep parsing and replacing the rows.
      NExt differs in finding the version: list the provisions instead of following a redirect,
      pick the one valid today (older and next versions are provided as well),
      and cope with the download confirmation after which a file may no longer be available.
