# Documentation Build

This Gradle task generates curated documentation PDFs:

```shell
./gradlew generateDocumentation
```

This task requires additional tooling, see [Dockerfile](../Jenkins/jenkins-agent/Dockerfile).

The generated files are written to:

```text
build/doc
```

The task also creates:

```text
build/doc/index.html
```

Only Markdown files listed in `documentationMarkdownSources` in `gradle/documentation.gradle.kts` are converted.
Add new documentation PDFs by adding the Markdown path, relative to `doc/`, to that list.
