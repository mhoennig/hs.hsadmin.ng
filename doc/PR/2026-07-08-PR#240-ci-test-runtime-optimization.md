# PR#240: CI Test-Runtime Optimization

## Related Links (Hostsharing-internal)

- Taiga-Story: n/a
- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/240

## The Problem

A full CI build on the GitTally build server takes about 25 minutes, of which the `test` task alone takes almost 19 minutes.

An analysis of the build artifacts and a local measurement showed:

- All integration tests run sequentially in a single forked JVM.
- The CI build command contains `--rerun-tasks`, which is redundant because `GITTALLY_BUILD_CLEAN_COMMAND='rm -rf build'` already wipes all task outputs before every build.
- About 6 of the 19 minutes are caused by pathologically slow coop-assets queries (eager self-referencing `@OneToOne` associations join-fetched through the function-backed `coopassettx_rv` view, measured at ~4.3s per row-load).
  This issue needs a separate investigation.

The coop-assets query problem also affects production and will be fixed in a separate follow-up PR.
This PR only addresses the mechanical CI runtime levers.

## The Requirements

- The `test` task utilizes the build server (4 cores, 12 GiB RAM) better by running integration tests in parallel.
- Parallel test JVMs must not share a database, so each fork gets its own Testcontainers PostgreSQL instance.
- Redundant Gradle flags are removed from the CI build command.
- The effect is measured by comparing the CI build duration against the 24m24s baseline of 2026-07-07.

## Background

I had already tried to parallelize build myself before, specifically parallelized the Gradle tasks.
To my surprise, but that had no effect at all.

The expensive tests need the Liquibase initialization —
which is very slow by now, because we already have so many changesets.
I wanted to avoid multiple of these initializations.

Therefore, I tried it just on the Gradle task level, not on the test level

(In hindsight, it turned out, however, that what is exactly the right approach.)

## The Solution

- The `test` task in the Gradle plugin `HsadminTestTasksPlugin.kt` now takes `maxParallelForks`
  from the environment variable `HSADMINNG_TEST_MAX_PARALLEL_FORKS` (default 3, also set in `.tc-environment`).
  Thus, each fork starts its own PostgreSQL container via the `jdbc:tc:` URL, so the forks are fully isolated.
- Removed `--rerun-tasks` from `GITTALLY_BUILD_COMMAND` in `.gitTally`.

Measured CI build durations (baseline 25 min):

| `maxParallelForks` | CI-build | Comment                           | 
|-------------------:|---------:|-----------------------------------|
|                  1 |   25 min | status quo before the change      |
|                  2 |   20 min |                                   |
|                  3 |   15 min | fastest => choosen for current VM |
|                  4 |   18 min |                                   |

## Additional Changes

None.
