# Documentation Build

Both builds described here are configured in
[buildSrc/src/main/kotlin/hsadmin.documentation.gradle.kts](../buildSrc/src/main/kotlin/hsadmin.documentation.gradle.kts).

## Documentation PDFs

This Gradle task generates curated documentation PDFs:

```shell
./gradlew generateDocumentation
```

This task requires additional tooling, see [Dockerfile](../Jenkins/jenkins-agent/Dockerfile).
It renders through [tools/markdown-to-pdf](../tools/markdown-to-pdf), thus it also needs `xelatex`.

The generated files are written to:

```text
build/doc
```

The task also creates:

```text
build/doc/index.html
```

Only Markdown files listed in `documentationMarkdownSources` are converted.
Add new documentation PDFs by adding the Markdown path, relative to `doc/`, to that list.

## Documentation Bundled Into the Jar

The application serves this documentation at `/doc/index.html`, without authentication, because it
is the same content as in the public repository.
The entry page is an overview which links to one page per part, so that no part buries the others,
and it offers the whole documentation as a ZIP, so that nobody has to unpack the jar.
Each part carries an icon and a note which tell whether it tracks the code or can have aged.

### What it contains

- selected business documentation from this directory,
- the generated RBAC diagrams from the Liquibase changelogs,
- the generated scenario-test reports,
- the REST API per module.

Only the Markdown files listed in `bundledDocumentationSources` are taken from this directory.
That list is deliberately short: it holds only documents which are still useful without access to
the source tree.
Likewise only the API definitions listed in `bundledApiSpecs` are rendered; `test/test.yaml`
describes test-only entities and stays out.

### How to build it

Every `bootJar` puts the Markdown sources into the jar, thus a plain build needs no documentation
tooling.
The scenario reports are only there if `gw scenarioTest` ran before.

To get the documentation as HTML pages with rendered Mermaid and PlantUML diagrams:

```shell
gw bootJarWithDocumentation
```

This wraps `bootJar`, runs `scenarioTest` first, and renders each document through
[tools/markdown-to-html](../tools/markdown-to-html) and each API module through
[tools/openapi-to-html](../tools/openapi-to-html).
Rendering needs pandoc, mermaid-filter, pandoc-plantuml, redocly and redoc, see
[Dockerfile](../Jenkins/jenkins-agent/Dockerfile); unlike the PDFs it needs no `xelatex`.
A build without the rendering says so on the overview page, because the Markdown sources download
instead of being shown.

The rendered pages embed their diagrams and their stylesheet as data-URIs, thus they need no
network access.

### The REST API

The API documentation comes in two forms.

**Swagger UI** at `/swagger-ui/index.html` offers the hand-written API definitions per module.
It reads them from `/doc/api/spec/`, where `DocumentationWebConfig` serves the whole
`api-definition` tree from the classpath, and resolves the `$ref`s between those files itself.
Serving single files instead would break that resolution, thus the tree is served as a whole,
including `test/test.yaml`, which is simply not linked anywhere.
This form needs no build tooling and therefore works in every build, including `gw bootRun`.

The API which the controllers actually implement stays available as the last entry of the selection
list, named "as implemented (generated from controllers)".
It carries no descriptions, because they get lost on the way through the generated interfaces.

**Static [Redoc](https://redocly.com/) pages** per module below `/doc/api/`, rendered by
`bootJarWithDocumentation`.
Each page embeds its bundled specification and loads `redoc.standalone.js` from beside it, so that
one copy of the 1.1 MB renderer serves all pages.
The bundled specification is kept as a single JSON file next to each page, for import into API
client tools.
These six pages carry neither the header nor the footer of the other pages, because Redoc takes
over the whole viewport; they are reached from the overview and lead back through the browser only.

### Corporate identity

These files carry the corporate identity of Hostsharing eG:

- `.documentation.css` — the palette, which also applies to the rendered documents,
- `.documentation-footer.html` — the legal links, put behind every rendered document,
- `.hostsharing-logo.svg` — taken from <https://www.hostsharing.net/images/logos/hostsharing-logo.svg>.

The breadcrumb in front of every rendered document is generated per section, because it names the
section the document belongs to.
