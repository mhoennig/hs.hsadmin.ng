# PR#256: Spring-Boot Test-Runtime Optimization

## Related Links (Hostsharing-internal)

- Taiga-Story: n/a
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/256

## The Problem

The integration test tasks run longer than necessary.
Two causes are independent of each other.

**Cause 1: DEBUG logging leaks into all test classes of a Gradle worker.**

`PingControllerAcceptanceTest` and `JwtAuthenticationFilterIntegrationTest` set `logging.level.root=DEBUG`
in a `@TestPropertySource`.
Spring Boot configures the `LoggingSystem` per JVM, not per application context.
A later context does not reset the root level.
From the first of these two test classes on, every following test class in the same worker logs at DEBUG.

The log of a `generalIntegrationTest` run shows this:
the first `org.postgresql.jdbc.PgConnection` DEBUG line appears 1.3 seconds
after `JwtAuthenticationFilterIntegrationTest` starts.
Before that point there is none.

Both properties have no effect on the tests themselves:
`WebEnvironment.RANDOM_PORT` already selects the port, so `server.port=0` is redundant.
`JwtAuthenticationFilterIntegrationTest` also declares `@ExtendWith(OutputCaptureExtension.class)`,
but it never reads a `CapturedOutput`.

A measurement of `generalIntegrationTest` with `--rerun-tasks`,
before and after the removal of both `@TestPropertySource` blocks:

|                       | wall-clock | test-result XML | contexts |
|-----------------------|-----------:|----------------:|---------:|
| before                |       5:51 |           69 MB |       11 |
| after                 |       4:12 |          4.1 MB |       10 |

**Cause 2: `generalIntegrationTest` creates too many application contexts.**

The Spring test context cache holds one context per merged configuration.
The tag-scoped test tasks reuse their contexts very differently:

| Gradle task              | DB-backed test classes | contexts |
|--------------------------|-----------------------:|---------:|
| `generalIntegrationTest` |                     24 |       11 |
| `officeIntegrationTest`  |                     22 |        2 |
| `bookingIntegrationTest` |                      4 |        2 |
| `hostingIntegrationTest` |                      3 |        3 |

The first context of a task costs about 40 seconds, because it starts the container and runs Liquibase.
Each further context costs only 1.5 to 4.7 seconds, because it reuses the migrated database.
The additional contexts therefore cost less than the DEBUG logging.
They still cost about 20 seconds.

Three details split the cache keys:

- `ContextBasedTest` carries `@Import(RbacGrantsDiagramService.class)`.
  `ImportsContextCustomizerFactory` searches the full type hierarchy, so this import becomes part of the cache key.
  The acceptance tests therefore split into 15 classes that inherit from `ContextBasedTest*` and 7 that do not.
  For `@SpringBootTest` the import is unnecessary, because `RbacGrantsDiagramService` is a `@Service` in the component scan.
- Four `@DataJpaTest` classes use a different `@Import` set than the other 22.
  `PostgresArrayIntegrationTest` has no import.
  `RbacRoleRepositoryIntegrationTest` and `ContextIntegrationTests` do not extend `ContextBasedTest`.
  `RealSubjectRepositoryIntegrationTest` misses `JpaAttempt`.
- `ContextIntegrationTests` carries `@DirtiesContext` without a documented reason.

Two more findings came out of the same analysis:

- No test configuration limits the HikariCP pool size, so each cached context holds up to 10 connections.
  The shutdown log of `generalIntegrationTest` shows `HikariPool-1` to `HikariPool-10` open at the same time.
  The Testcontainers PostgreSQL allows `max_connections=100`, so the run has no reserve left.
  The `test` task collects up to 14 contexts per fork.
  This can cause the "Testcontainers-PostgreSQL problem spillover" that `.aliases` mentions.
- `gw-test --all` in `.aliases` runs `unitTest`, the three module tasks, `scenarioTest` and `migrationTest`,
  but it does not run `generalIntegrationTest`.
  So 24 test classes stay out of the full local test run.

## Non-Goals

- No change of test semantics, of assertions or of test data.
- No new test framework and no new test base class hierarchy.
- No optimization of the slow test classes themselves, for example `RbacSubjectRepositoryIntegrationTest`.
- No change of the 25 `@WebMvcTest` contexts.
  Each of them binds a different controller and a different mock set, so one context per class is unavoidable.
  These contexts need no database and belong to the fast `unitTest` task.

## The Requirements

- The test runs produce log output at INFO level, unless a test class explicitly needs more.
- A test class does not change the logging of the test classes that run after it.
- Test classes with an equal technical setup share one application context.
- The cached contexts of one Gradle worker stay clearly below `max_connections` of the database container.
- `gw-test --all` covers all test tags.
- Step 6 measures the effect against the baseline of 5:51 for `generalIntegrationTest`.

## Background

`generalIntegrationTest` was the only task with a bad ratio of contexts to test classes.
This looked like the main problem at first.
The A/B measurement showed a different result.
Liquibase runs only once per worker, so an additional context is cheap after the first one.
The expensive part was the DEBUG logging.
Two annotation lines switched it on for the whole worker.

For this reason the plan below fixes the logging first.
It treats the context unification as a separate step.
Both steps are useful, but their ratio of cost to benefit differs strongly.

## The Solution

This PR splits the work into eight steps.
Steps 1 to 4 are small and independent.
Step 5 touches many test classes, so it is the last of the planned code steps.
Step 6 measures.
Steps 7 and 8 were not planned.
Each of them follows from the previous measurement.
Both leave the test semantics unchanged.

| step | what                                                           | measured effect                                       |
|-----:|----------------------------------------------------------------|-------------------------------------------------------|
|    1 | remove the `@TestPropertySource` blocks with `root=DEBUG`      | `generalIntegrationTest` 5:51 -> 4:22, 69 MB -> 4.1 MB |
|    2 | set the dsproxy query log to `WARN`                            | `generalIntegrationTest` 4:22 -> 2:56, 4.1 MB -> 1.3 MB |
|    3 | limit the HikariCP pool to 3 connections per context           | no runtime effect; removes the `max_connections` risk  |
|    4 | add `generalIntegrationTest` to `gw-test --all`                | no runtime effect; 24 classes join the full local run  |
|    5 | unify the `@DataJpaTest` contexts via `@RbacDataJpaTest`        | `generalIntegrationTest` 11 -> 6 contexts              |
|    6 | measure                                                        | -                                                     |
|    7 | take the coop-assets tests off the query PR#247 replaced        | `officeIntegrationTest` 11:45 -> 8:27                  |
|    8 | fetch the coop-assets self-links lazily                          | `scenarioTest` 44s -> 22s, reversal `POST` 8577 -> 233ms |

### Step 1: Remove the dead test properties

- Delete the `@TestPropertySource` block in `PingControllerAcceptanceTest` and in `JwtAuthenticationFilterIntegrationTest`.
- Delete the now unused `TestPropertySource` imports.
- Delete `@ExtendWith(OutputCaptureExtension.class)` and its import in `JwtAuthenticationFilterIntegrationTest`,
  because no test method reads a `CapturedOutput`.
- Both classes then join the common acceptance-test context.

Measured: `gw-test generalIntegrationTest --rerun` finished in 4:22 with 4.1 MB in `build/test-results` and 10 contexts.

### Step 2: Switch off the SQL query log by default

- Set `net.ttddyy.dsproxy.listener` to `WARN` in `src/test/resources/application.yml`.
- Keep the `HOWTO` comment, so a developer can switch the level back to `DEBUG` for a local analysis.

Measured: the run wrote no `SLF4JQueryLoggingListener` record anymore, where the run of step 1 still contained 2647 of them.
`gw-test generalIntegrationTest --rerun` dropped from 4:22 to 2:56 and from 4.1 MB to 1.3 MB.

### Step 3: Limit the connection pool of the test contexts

- Add `spring.datasource.hikari.maximum-pool-size: 3` to `src/test/resources/application.yml`.
- The integration tests run single-threaded per worker, so three connections per context are enough.

Measured: `gw-test --quick` and `gw-test generalIntegrationTest` passed without a connection error.

### Step 4: Add the missing task to the full test run

- Add `generalIntegrationTest` to the `--all` branch of `_gwTest` in `.aliases`.
- Put it into the same `gradlew` call as the three module tasks, because it needs the same kind of database.

Measured: `gw-test --all` reported test results for `generalIntegrationTest`.

### Step 5: Unify the `@DataJpaTest` contexts

- Remove `@Import(RbacGrantsDiagramService.class)` from `ContextBasedTest`.
  The `@SpringBootTest` classes get the bean from the component scan, so they lose nothing.
- Introduce the meta-annotation `@RbacDataJpaTest` for the repository tests.
  It carries `@DataJpaTest` and the three imports `Context`, `JpaAttempt` and `RbacGrantsDiagramService`.
  It also carries `@MockitoBean(types = HttpServletRequest.class)`,
  because a bean override also belongs to the cache key.
  25 of the 26 classes declared exactly this mock and never used it.
- Replace the per-class `@DataJpaTest`, `@Import` and `@MockitoBean` lines with the new annotation in the non-migration repository tests.
- Keep the plain `@DataJpaTest` of `PostgresArrayIntegrationTest`.
  The architecture rule `everythingShouldBeFreeOfCycles` forbids a dependency from the `mapper` package to `rbac`,
  and this test needs no RBAC bean at all.
- Add `RbacGrantsDiagramService.class` to the `@Import` of `ImportHostingAssets`.
  This test extends `ContextBasedTest` through `CsvDataImport`,
  and `ContextBasedTest` autowires the bean without `required = false`.
  It cannot use the new meta-annotation, because it needs its own `@DataJpaTest` properties.
- Remove the `@DirtiesContext` of `ContextIntegrationTests`.
  Nothing in the class leaves state in the application context,
  and the annotation would now evict the shared context for all classes that run afterwards.
- Keep the separate contexts of `ProvisioningApiKeyBootstrapAcceptanceTest`, `WebSecurityConfigIntegrationTest`,
  `MessageTranslatorIntegrationTest`, `ScenarioTest` and both migration tests.
  These need their own properties, profiles or database.

Measured with `grep -ho "Started [A-Za-z]* in [0-9.]* seconds" build/test-results/<task>/*.xml | sort -u`:
`generalIntegrationTest` now creates 6 contexts instead of 11.
The remaining five besides the shared one belong to `CustomActuatorEndpointAcceptanceTest`,
`MessageTranslatorIntegrationTest`, `PostgresArrayIntegrationTest`,
`ProvisioningApiKeyBootstrapAcceptanceTest` and `WebSecurityConfigIntegrationTest`.

### Step 6: Measure again and record the result

`gw-test generalIntegrationTest --rerun`, the task that motivated this PR:

|                | wall-clock | test-result XML | contexts |
|----------------|-----------:|----------------:|---------:|
| before         |       5:51 |           69 MB |       11 |
| after step 1   |       4:22 |          4.1 MB |       10 |
| after step 2   |       2:56 |          1.3 MB |       10 |
| after step 5   |       2:57 |          1.3 MB |        6 |

`gw-test --all --rerun` takes 23:03 in total and is green.
The durations come from the Gradle test reports, the context counts from the test-result XML:

| Gradle task              | duration | test classes | contexts |
|--------------------------|---------:|-------------:|---------:|
| `unitTest`               |    0:24  |          185 |       25 |
| `officeIntegrationTest`  |   11:45  |          107 |        2 |
| `bookingIntegrationTest` |    1:32  |           20 |        2 |
| `hostingIntegrationTest` |    0:54  |           11 |        2 |
| `generalIntegrationTest` |    1:51  |           61 |        6 |
| `scenarioTest`           |    0:38  |           19 |        1 |
| `migrationTest`          |    0:23  |            2 |        2 |

The 25 contexts of `unitTest` are the `@WebMvcTest` contexts, which are a non-goal of this PR.
The task-durations do not add up to the total, because each `gradlew` call starts its own container and runs Liquibase.

The CI build on GitTally, which runs `jacocoTestReport check generateDocumentation` instead of the tag-scoped tasks:

| commit     | steps | CI wall-clock | `test` task |
|------------|-------|--------------:|------------:|
| `960e0ad5` | none, master baseline |  12:15 |    14:08 |
| `9167a135` | 1 to 3                |  10:57 |    13:29 |
| `bf17e2c1` | 4                     |  11:05 |    13:19 |
| `1ee4582a` | 5                     |  11:18 |    13:50 |

Only steps 1 and 2 are visible here.
Step 4 changes `.aliases`, which the CI does not use.
Its build therefore measures an unchanged workload, and its deviation of 10 seconds shows the variance of the build server.
The 32 seconds between step 4 and step 5 consist of 18 seconds in `FindAllCoopAssetsTransactions` alone.
The duration of that class varies between 141 and 159 seconds over these four builds, without any relation to the commits.
The context unification also shows little in the CI.
`check` runs every test class in each build, and each of the three forks builds only the contexts of its own classes.

### Step 7: Take the coop-assets tests off the query which PR#247 replaced

The measurement of step 6 showed `officeIntegrationTest` as the slowest task.
282 of its 703 seconds came from seven calls of one repository method.

`findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange` joins `coopassettx_rv` to itself up to five times.
PR#247 replaced it in the controller by `findVisibleUuidsByOptionalMembershipUuidAndDateRange` plus the raw link graph.
It did not migrate the tests, so the method kept no caller in `src/main` and seven in `src/test`.

This also left a gap in the test coverage.
`ListCoopAssetsTransactions` runs as global-agent.
Both listing tests of the acceptance test are `globalAdmin_...`,
and `HsOfficeCoopAssetsTransactionControllerRestTest` mocks both repositories.
So no test verified that the query which production really uses is RBAC-gated.

- Let the four tests of `FindAllCoopAssetsTransactions` call the same pair as the controller:
  `findVisibleUuidsByOptionalMembershipUuidAndDateRange` and then `HsOfficeCoopAssetsTransactionRealRepository`.
  `partnerPersonAdmin_canViewRelatedCoopAssetsTransactions` now guards the production query.
- Let the three setup lookups of `GetCoopAssetTransaction` use the same uuid projection.
  They only need a uuid, and the endpoint under test is the fast path.
- Give `HsOfficeCoopAssetsTransactionRealEntity` the `Stringify` of its Rbac twin, under the explicit name
  `CoopAssetsTransaction`, so the expected values of the tests stay unchanged.
  A `TODO.refa` marks it, because both twins belong under a shared `@MappedSuperclass`, as `HsOfficeRelation` has one.
- Delete the old query; after the migration it has no caller left at all.

Measured, and the red/green proof of the coverage:

| test class                                                    | before | after |
|---------------------------------------------------------------|-------:|------:|
| `HsOfficeCoopAssetsTransactionRepositoryIntegrationTest$FindAllCoopAssetsTransactions` | 208.3s |  7.7s |
| `HsOfficeCoopAssetsTransactionControllerAcceptanceTest$GetCoopAssetTransaction`        |  73.5s |  9.1s |
| `officeIntegrationTest` in total                                                       |  11:45 |  8:27 |

Changing the entity in `findVisibleUuidsByOptionalMembershipUuidAndDateRange`
from `HsOfficeCoopAssetsTransactionEntity` to `HsOfficeCoopAssetsTransactionRealEntity`,
which removes the RBAC gate, makes exactly `partnerPersonAdmin_canViewRelatedCoopAssetsTransactions` fail.
Before this step the same change kept every test green.

### Step 8: Fetch the coop-assets self-links lazily

After step 7 the four slowest tests of the whole suite were coop-asset writes.
Together they needed about 45 seconds:
`shouldRevertCoopAssetsTransferIncludingRelatedAssetAdoption` 16.3s,
`globalAdmin_canAddCoopAssetsReversalTransaction` 10.6s,
`globalAdmin_canCreateNewCoopAssetTransaction` 10.5s
and `shouldRevertCoopAssetsSubscription` 8.2s.

The term "write path" does not describe the cause correctly.
A run with `LOGGING_LEVEL_NET_TTDDYY_DSPROXY_LISTENER=DEBUG` shows where the time goes:

| test                                                | statements | selects with self-joined `coopassettx_rv` |
|-----------------------------------------------------|-----------:|------------------------------------------:|
| `createsAndGrantsRoles`, which only calls `save()`   |       1196 |                                    0, 0ms |
| `PostNewCoopAssetTransaction` through the endpoint   |       1263 |                                 2, 8699ms |

So the insert through the `INSTEAD OF` trigger of `coopassettx_rv` produces none of these selects.
The creation of the RBAC roles and grants produces none of them either.
Two reads on the `POST` path cause the cost.
Their query has the same form which PR#247 removed from the listing:

```sql
from hs_office.coopassettx_rv hocate1_0
  left join hs_office.coopassettx_rv aat1_0 on aat1_0.uuid = hocate1_0.assetadoptiontxuuid
  left join hs_office.membership_rv     m1_0 on m1_0.uuid  = aat1_0.membershipuuid
  left join hs_office.coopassettx_rv rat1_0 on aat1_0.uuid = rat1_0.revertedassettxuuid
  ...
```

- `RESOURCE_TO_ENTITY_POSTMAPPER` resolves the referenced transaction
  with `coopAssetsTransactionRepo.findByUuid(resource.getRevertedAssetTxUuid())`.
- `ENTITY_TO_RESOURCE_POSTMAPPER` renders the response from the saved `_rv` entity
  and thereby reads `getReversalAssetTx()`, `getAdoptionAssetTx()` and `getTransferAssetTx()`.

The tests are not the only place with this cost.
A `POST` of a reversal transaction also costs about 4.5 seconds twice in production.

Neither read may move off the restricted view.
The insert has to stay on `coopassettx_rv`, because its `INSTEAD OF` trigger enforces the RBAC permission.
The RbacSpec grants that permission with `.toRole("membership", ADMIN).grantPermission(INSERT)`.
And `findByUuid` in `RESOURCE_TO_ENTITY_POSTMAPPER` serves as an authorization check, which its name does not show.
It returns empty if the subject may not see the referenced transaction,
and the caller then throws an `EntityNotFoundException`.
The write logic also needs the `_rv` entity itself,
because `adoptionAssetTx.setReversalAssetTx(...)` persists the counterpart through its `cascade = CascadeType.PERSIST`.

So the fix neither moves the read nor removes it.
It only stops the unnecessary fetch of more rows:

- Fetch the two owning-side self-links, `revertedAssetTx` and `adoptionAssetTx`, lazily.
- Leave the two `mappedBy` sides as they are.
  Hibernate cannot proxy a nullable inverse `@OneToOne` without bytecode enhancement, so `LAZY` does not help there.

`EAGER` on a self-referential association applies to every level.
The eager links of the loaded row fetch their own eager links, and those fetch theirs,
until the graph has no more links.
The same test with both fetch strategies:

| fetch strategy   | selects | `coopassettx_rv` in the `FROM` | `membership_rv` |   time |
|------------------|--------:|-------------------------------:|----------------:|-------:|
| `EAGER` (before) |       2 |                            13x |             13x | 8577ms |
| `LAZY` (after)   |       2 |                             5x |              5x |  233ms |

The number of statements is the same in both cases, 1205 in total and 38 of them on `coopassettx`.
The change saves no query, and no link stays unresolved.
Only the width of the join changes.
Every occurrence of `coopassettx_rv` in the `FROM` evaluates the recursive accessible-object-uuids CTE
of the restricted view again.
Therefore 13 occurrences instead of 5 cost a factor of 37, not a factor of 2.6.
The root lookup with `where uuid=?` costs 2ms.

The business domain only needs a resolution of one level.
A reversal refers to the transaction which it reverts, and a transfer to its adoption counterpart.
No use case asks for the links of the links.

`CreateCoopAssetsRevertTransferTransaction` verifies this.
It reads `revertedAssetTx.uuid` and `revertedAssetTx.['adoptionAssetTx.uuid']` out of the `POST` response.
The second one is a lazy link in a lazy link.
The test uses both values for follow-up requests.
`globalAdmin_canAddCoopAssetsReversalTransaction` asserts the nested `revertedAssetTx` object of the `POST` response.
Both tests fail on a `LazyInitializationException` and on a link which the response does not contain.

The measured effect, where the coop-assets classes are the six of `officeIntegrationTest`:

| test time              | before step 7 | after step 7 | after step 8 |
|------------------------|--------------:|-------------:|-------------:|
| coop-assets classes    |        306.3s |        52.2s |        27.7s |
| `scenarioTest`         |           44s |          44s |          22s |
| `officeIntegrationTest`|          703s |         451s |         464s |

`scenarioTest` needs half of its former time, because its use cases run the `POST` path very often.
The total of `officeIntegrationTest` does not improve, although its coop-assets classes need 24.5s less.
The other 101 classes of that run needed about 37 seconds more than in the run before.
This is the variance of the machine, not an effect of this step.
One measurement per variant cannot separate a change of 5% from that variance.
Only the numbers per query are reliable, because they compare the same statements of the same test.

## The Remaining Distribution

No unusually slow class is left.
The slowest one, `HsOfficeDebitorRepositoryIntegrationTest`, needs 38.3 seconds, but for 23 tests.

| Gradle task              | test time | tests | per test | measured           |
|--------------------------|----------:|------:|---------:|--------------------|
| `officeIntegrationTest`  |      464s |   261 |    1.78s | after step 8       |
| `bookingIntegrationTest` |       92s |    47 |    1.96s | after step 6       |
| `hostingIntegrationTest` |       54s |    32 |    1.67s | after step 6       |
| `generalIntegrationTest` |      111s |   218 |    0.51s | after step 6       |
| `scenarioTest`           |       22s |    95 |    0.23s | after step 8       |
| `migrationTest`          |       23s |    73 |    0.32s | after step 6       |
| `unitTest`               |       23s |  1239 |    0.02s | after step 6       |

Steps 7 and 8 only change coop-assets code.
The four tasks measured after step 6 therefore still show a valid value.
A test over the restricted views costs about 1.8 seconds.
`officeIntegrationTest` is the largest task because it has the most tests, no longer because of single slow classes.

This also corrects an earlier note of this document.
That note claimed more than 30 seconds each for `RbacSubjectRepositoryIntegrationTest`
and `RbacSubjectControllerAcceptanceTest`, from a measurement before step 1.
They now need 17.7s for 21 tests and 18.1s for 38 tests.

The largest remaining cost is not a test.
All tests together need about 790 seconds, but `gw-test --all --rerun` needed 1383 seconds after step 6.
The difference of 9 to 10 minutes consists of the seven cold starts
and of the Gradle overhead of four `gradlew` calls.
See the appendix.

All wall-clock values and test times of this document have a limit.
Each of them is one measurement on a developer machine which ran builds one after the other.
The runs of steps 7 and 8 show the variance:
101 unchanged test classes differed by about 37 seconds between two runs.
Only the numbers per query are reliable, because they compare the same statements of the same test.

## Answered Questions

- Is the `@DirtiesContext` of `ContextIntegrationTests` needed?
  No.
  The class only changes the `SecurityContextHolder`, which is thread-local, and the RBAC context, which is session state of the database.
  Neither survives in the application context.
- Should step 5 use a meta-annotation or a common abstract base class?
  A meta-annotation, because it keeps the existing class hierarchy untouched.

## Follow-up PRs

- [ ] TODO.test: Reuse of the Testcontainers PostgreSQL between the test tasks.
  The appendix below holds the analysis; nobody did the work yet.
- [ ] TODO.refa: Unification of `HsOfficeCoopAssetsTransactionEntity` and `HsOfficeCoopAssetsTransactionRealEntity`
  under a shared `@MappedSuperclass`, as `HsOfficeRelation` has one for its two twins.
  This removes the duplicated `Stringify` of step 7.
  It can also declare the four self-links with the type of the raw entity, which is what PR#247 assumes.
  But it changes the `POST` path, because those links carry `cascade = CascadeType.PERSIST`,
  and it changes the `ORDER BY at.membership.memberNumberSuffix` of the queries.

## Appendix: Why the Testcontainers Reuse Is Not Part of This PR

`src/test/resources/application.yml` carries an unused `url-tcx` with `TC_REUSABLE=true&TC_DAEMON=true`,
next to the `url-tc` which `url` selects.
The idea looks simple, but it saves less time than expected, and it needs preparatory work of its own.

Gradle forks a JVM per test task, and a `jdbc:tc:` URL starts one container per JVM.
The cold start of a task contains the container and the full Liquibase changelog.
These are the measured values:

| Gradle task              | cold start |
|--------------------------|-----------:|
| `generalIntegrationTest` |      36.6s |
| `officeIntegrationTest`  |      35.3s |
| `bookingIntegrationTest` |      35.0s |
| `hostingIntegrationTest` |      33.3s |
| `scenarioTest`           |      33.0s |
| `migrationTest`          |      21.0s |

The reuse works only within one database, and there are four of them:

| database                     | used by                                     |
|------------------------------|---------------------------------------------|
| `spring_boot_testcontainers` | office, booking, hosting, general            |
| `scenariosTC`                | `ScenarioTest`                               |
| `importHostingAssetsTC`      | `ImportHostingAssets`                        |
| `liquibaseMigrationTestTC`   | `LiquibaseCompatibilityIntegrationTest`      |

Only the first group shares a container.
This saves three cold starts, about 1:45 of the 23:03 of `gw-test --all --rerun`.
The larger gain is the daily work, where every single `gw-test <task>` starts about 35 seconds faster.

Two conditions make this more than a change of the configuration:

- The repository cannot switch it on.
  Testcontainers ignores `TC_REUSABLE` unless the developer sets `testcontainers.reuse.enable=true`
  in `~/.testcontainers.properties`.
  And the CI must not have it, because a container which survives a build removes the isolation of the next one.
  So `url` must select `url-tcx` per machine, not always.
- A reused container keeps its data.
  Liquibase skips a schema which it already migrated, but committed test data stays.
  Many assertions cover a whole table.
  `globalAdmin_canViewAllCoopAssetsTransactions` for example expects exactly 18 rows.
  Tests with `@Transactional` roll back, but `jpaAttempt.transacted(...)` commits,
  which is the reason for `ContextBasedTestWithCleanup`.
  The actual work is to make every suite repeatable against a warm database.
  This also risks tests which depend on their order, and such tests cost more than 1:45.

`maxParallelForks` adds a second condition.
Today each fork gets its own container on purpose, see the comment in `HsadminTestTasksPlugin.kt`.
With the reuse the forks of one task share a database.

The same analysis found a cheaper measure.
The `test` task, which the CI runs, includes the tags `migrationTest`, `generalIntegrationTest`,
`officeIntegrationTest`, `bookingIntegrationTest` and `hostingIntegrationTest` in three forks.
It therefore pays three cold starts for all of them.
`gw-test --all` runs four separate tasks and pays four, plus those of `scenarioTest` and `migrationTest`.
The tag-scoped split costs one cold start which `test` does not need.
In exchange it gives reports per module and isolation.
