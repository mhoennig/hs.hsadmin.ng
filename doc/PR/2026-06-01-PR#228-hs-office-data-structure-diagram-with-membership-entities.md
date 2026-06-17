# PR#228: hs-office-data-structure diagram with membership entities

## Related Links (Hostsharing-internal)

- GitEA-PR: https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/228

## The Problem

The office data-structure diagram documented partners, relations, debitors, representatives and operations contacts,
but it did not show the cooperative membership model.

This made the diagram incomplete for understanding how partners relate to memberships, cooperative share transactions
and cooperative asset transactions.

## The Solution

The Mermaid diagram in `doc/hs-office-data-structure.md` now includes the membership area.
The document also got PDF frontmatter so the generated diagram PDF uses an A2 landscape layout with Mermaid scaling.

The added membership area contains:

- membership linked to partner
- cooperative share transaction linked to membership
- cooperative asset transaction linked to membership
- reversal and adoption self-links for transaction references
- membership, cooperative share and cooperative asset enum values in the membership scope

The enum types are represented as separate nodes and linked from the nodes where they are used.

## Additional Changes

### Renaming /bin to /tools

Repository utility scripts were moved from `/bin` to `/tools`.
The reason for this is that `/bin` is in `.gitignore`
because some IDEs use it as an output dir for internal builds.

`tools/howto`, `tools/jwt-curl` and `tools/system-summary` are now under `/tools`.
`tools/markdown-to-pdf` was added as the PDF conversion helper.
`bin/git-watch-origin-and-test` was removed, as it got replaced by `gitTally`.

### Improved Documentation PDF Generation in Gradle 

- `./gradlew generateDocumentation` generates curated documentation PDFs into `build/doc`
- recursive `index.html` files are generated under `build/doc`
- PDF links open in a new tab/window, nested index links open in the same tab/window
- scenario-test reports get their own `build/doc/scenarios/index.html`, linked from the root documentation index
- the converted Markdown files are controlled by `documentationMarkdownSources` in
  `buildSrc/src/main/kotlin/hsadmin.documentation.gradle.kts`
- `doc/README.md` documents the documentation build

The Gradle documentation tasks got extracted to `buildSrc/src/main/kotlin/hsadmin.documentation.gradle.kts`.
The test-task and quality-tooling setup got extracted to the `hsadmin.test-tasks`
and `hsadmin.quality` buildSrc convention plugins
to keep the root build script smaller.

### Avoiding Re-Testing

The timestamp in the Spring boot build.info caused tests to run too often, even no real dependency changed.

Now the timestamp is omitted for normal development and test runs to avoid invalidating test tasks.
Use `./gradlew bootJarWithBuildTime` for building the executable jar with `build.time` included.
This got also documented in the `README.md`. 

### Configurable Scenario-Test HTTP Timeout

One of the scenario tests failed repeatedly due to an HTTP timeout.
The scenario-test HTTP timeout is now configurable via `HSADMINNG_SCENARIO_HTTP_TIMEOUT_SECONDS`.
The Testcontainers environment and the Java fallback set it to `30` now.

## Verification

The documentation PDF generation was verified with:

```shell
./gradlew generateDocumentation
```

The generated office data-structure PDF is a single A2 page, and the generated documentation indexes link to all generated PDFs.
