## Test-Concept

<!-- generated TOC begin: -->
- [Unit-Tests](#unit-tests)
- [REST-Tests](#rest-tests)
- [Integration-Tests](#integration-tests)
- [Acceptance-Tests](#acceptance-tests)
- [Performance-Tests](#performance-tests)
- [System-Integration-Tests](#system-integration-tests)
<!-- generated TOC end. -->

### General Issues

The following test concept uses the terms "double" and "mock" (maybe in inflected form like "mocking" or "mocked"), "whitebox-test" and "blackbox-tests" which I would like to define first.

#### Test-Doubles, Dummies, Fakes, Mocks, Spies and Stubs

A "double" is a general term for something which replaces a real implementation of a dependency of the unit under test.
This can be a "dummy", a "fake", a "mock", a "spy" or a "stub".
Often the term "mock" is used in a generic way, because typical mocking libraries like *Mockito* can also be used as dummies or spies and can replace fakes.

A fake would be a double without using any library, but rather a manual fake implementation of a dependency.
Where our APIs should be designed in a way that it's possible, using a mocking library like *Mockito* often leads to shorter test code.

#### Whitebox- and Blackbox-Tests

A whitebox-test knows and considers the internals of an implementation, e.g. it knows which dependencies it needs and can test special, implementation-dependent cases.

A blackbox-test does not know and not consider such internals of an implementation, it just tests externally observable behaviour.

### Kinds of Tests

Depending on the concrete aspects which we want to test, we are using different kinds of tests as described as follows.

#### Unit-Tests

In this project a *Unit* for *UnitTests* can be a single method (function), a class or even a group of classes which express a common concept.
The unit are whitebox-tests and count into test-code-coverage.

Unit-Test in this project are implemented with *JUnit Jupiter*, *Mockito* and *AssertJ*.

Unit-Tests do not use any external systems, not even a database.
They just test the unit, not any dependencies or proper integration with dependencies.

Such tests usually run very fast and should test all branches.

These Tests are always named `...UnitTest` and can automatically run in the build-process.


#### REST-Tests

At the level of REST-Controllers, *Spring's* `WebMvcTest`, a special kind of Unit-Test, are utilized.
Such tests issue REST-requests through a mocked REST-Layer and therefore use the controllers similar to a real client.
Otherwise, the implementation technologies are like those of Unit-Tests.

Being unit-tests, also REST-tests are whitebox-tests and count into test-code-coverage.

Like other Unit-Tests, REST-Test do not use any external systems, not even a database.
They just test the REST-related parts of the unit, e.g. URL-Mappings, HTTP-Headers and proper JSON encoding of request and response data.
Other dependencies and integrations with such are not tested on this level.

Such tests usually run very fast, but should focus on REST-specific issues, leaving branch-testing to pure Unit-Tests.

These Tests are always named `...RestTest` and can automatically run in the build-process.


#### Integration-Tests

Integration-Tests in this context mean integration with support systems like databases or messaging-systems, but not integration with external systems.

Integration-tests, are blackbox-tests and do <u>not</u> count into test-code-coverage.

Such tests are implemented with *JUnit Jupiter* through some sort of `@SpringBootTest`, e.g. `DataJpaTest` and usually utilize *Testcontainers* and *Docker* to wrap the supporting system, e.g. the *PostgreSQL* database.
*Mockito* can also be used for this kind of tests, to separate multiple integrations.

Integration-Tests are relatively slow and therefore should focus on the integration.
Internal issues should be tested through Unit-Tests.

These Tests are always named `...IntegrationTest` and can automatically run in the build-process.


#### Acceptance-Tests

We define Acceptance-Tests as test which describe user-stories, respectively high-level business requirements.
Acceptance-Tests run on a fully integrated and deployed system with deployed doubles for external systems.

Acceptance-tests, are blackbox-tests and do <u>not</u> count into test-code-coverage.

TODO: Complete the Acceptance-Tests test concept.


#### Performance-Tests

Performance-critical scenarios have to be identified and a special performance-test has to be implemented.

The implementation-technologie depends on the scenario.

Performance-tests, are blackbox-tests and do <u>not</u> count into test-code-coverage.

Such tests usually are very slow and should not be automatically run in the build-pipeline but manually, after critical areas have been changed.


#### System-Integration-Tests

We define System-Integration-Tests as test in which this system is deployed in a production-like environment to test integration with external systems.

System-Integration-tests, are blackbox-tests and do <u>not</u> count into test-code-coverage.

TODO: Complete the System-Integration-Tests test concept.
