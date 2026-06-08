# PR#227: Base JaCoCo verification on unit tests

## The Problem

JaCoCo coverage verification was coupled to the default `test` task.
That task can include slow tests which require PostgreSQL via Testcontainers:

- integration tests
- acceptance tests
- migration+import tests
- scenario tests

This made the coverage gate too slow for regular use and unsuitable as a basis for re-activating PiTest.
Several production classes also failed the stricter class-level JaCoCo coverage rule.
Running all tests took about 20 min even on a fast machine.

## The Solution

`jacocoTestCoverageVerification` now depends on the custom `unitTest` task.
This task excludes tests which need a database, while still running architecture tests and REST controller tests based on `@WebMvcTest`.

Focused unit tests were added for:

- controllers
- translations
- entities
- config classes
- mapper helpers
- RBAC support classes

Controller tests use `@WebMvcTest(...)`; non-controller tests use plain unit tests.
`EntityManagerWrapperFake` is now used where a lightweight in-memory persistence fake fits better than method-by-method mocks.

The unit-test coverage gate now runs in about 1 min.

## Additional Changes

The touched tests use Lombok `val` for inferred local variables.
The local `git-watch-origin-and-test` helper was improved, especially to be able to run gradle in a Docker container.

## Verification

Checked locally:

```bash
. .tc-environment; ./gradlew test --tests 'net.hostsharing.hsadminng.hs.booking.item.HsBookingItemControllerRestTest*' --tests 'net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetControllerRestTest*'
. .tc-environment; ./gradlew spotlessJavaCheck -x test
. .tc-environment; ./gradlew jacocoTestCoverageVerification
. .tc-environment; ./gradlew clean test check -x pitest -x dependencyCheckAnalyze --dry-run
. .tc-environment; ./gradlew jacocoTestCoverageVerification spotlessJavaCheck -x test
```
