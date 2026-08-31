import java.io.File

// The scenario tests write their Markdown reports here; build/reports/scenarios holds the HTML.
val scenarioReportMarkdownDir = layout.buildDirectory.dir("scenario-reports")

// Generate HTML from Markdown scenario-test-reports using Pandoc:
tasks.register("convertMarkdownToHtml") {
    description = "Generates HTML from Markdown scenario-test-reports using Pandoc."
    group = "Conversion"

    // Define the template file using project.file
    val templateFile = project.file("doc/scenarios/.template.html")
    inputs.file(templateFile).withPathSensitivity(PathSensitivity.NONE)

    // The scenario tests write the markdown reports outside any registered output directory,
    // otherwise Gradle's stale-output cleanup could delete them before this task runs.
    val inputDir = scenarioReportMarkdownDir
    val outputDir = layout.buildDirectory.dir("reports/scenarios")
    inputs.files(fileTree(inputDir) { include("*.md") }).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(outputDir)

    onlyIf {
        val dir = inputDir.get().asFile
        if (!dir.exists()) {
            logger.lifecycle("Skipping convertMarkdownToHtml because ${dir} does not exist (scenarioTest skipped).")
            false
        } else {
            true
        }
    }

    doFirst {
        // Check if pandoc is installed
        val proc = ProcessBuilder("pandoc", "--version")
            .redirectOutput(File.createTempFile("pandoc_check", ".out"))
            .redirectError(File.createTempFile("pandoc_check", ".err"))
            .start()
        proc.waitFor()
        if (proc.exitValue() != 0) {
            throw GradleException("Pandoc is not installed or not found in the system path. Please install Pandoc.")
        }

        // Check if the template file exists
        if (!templateFile.exists()) {
            throw GradleException("Template file '$templateFile' not found.")
        }
    }

    doLast {
        outputDir.get().asFile.mkdirs()
        // Gather all Markdown files in the input directory
        project.fileTree(inputDir) {
            include("*.md")
        }.forEach { file ->
            val outputFile = File(outputDir.get().asFile, file.name.replace(".md", ".html"))

            // Execute pandoc for each markdown file
            val p = ProcessBuilder(
                "pandoc",
                file.absolutePath,
                "--template", templateFile.absolutePath,
                "-o", outputFile.absolutePath
            ).inheritIO().start()
            p.waitFor()
            if (p.exitValue() != 0) throw GradleException("pandoc failed for ${file.name}")
            println("Converted ${file.name} to ${outputFile.name}")
        }
    }
    // Ensure this task runs after scenario tests have potentially generated the markdown files
    dependsOn(tasks.named("scenarioTest"))
}

// under build/reports the CI build-overview page links the report index directly, like the JUnit reports
val scenarioReportDir = layout.buildDirectory.dir("reports/scenarios")
val scenarioReportIndexFile = scenarioReportDir.map { it.file("index.html") }

tasks.register("generateScenarioTestIndex") {
    description = "Generates an HTML index linking to generated scenario-test reports."
    group = "Conversion"

    dependsOn(tasks.named("convertMarkdownToHtml"))

    val scenarioHtmlFiles = fileTree(scenarioReportDir) {
        include("*.html")
        exclude("index.html", ".*.html")
    }

    inputs.files(scenarioHtmlFiles).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(scenarioReportIndexFile)

    onlyIf {
        val dir = scenarioReportDir.get().asFile
        if (!dir.exists()) {
            logger.lifecycle("Skipping generateScenarioTestIndex because ${dir} does not exist (scenarioTest skipped).")
            false
        } else {
            true
        }
    }

    doLast {
        val indexFile = scenarioReportIndexFile.get().asFile
        indexFile.parentFile.mkdirs()

        val htmlFiles = scenarioReportDir.get().asFile
            .listFiles { file ->
                file.isFile &&
                        file.extension == "html" &&
                        file.name != "index.html" &&
                        !file.name.startsWith(".")
            }
            .orEmpty()
            .sortedBy { it.name }

        indexFile.writeText(buildString {
            appendLine("<!doctype html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("  <meta charset=\"utf-8\">")
            appendLine("  <title>Scenario Test Reports</title>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("  <h1>Scenario Test Reports</h1>")
            appendLine("  <ul>")
            htmlFiles.forEach { htmlFile ->
                appendLine("    <li><a href=\"${htmlFile.name}\">${htmlFile.name}</a></li>")
            }
            appendLine("  </ul>")
            appendLine("</body>")
            appendLine("</html>")
        })
    }
}

// --- documentation bundled into the application jar, served under /doc/ ------

// Only business documentation which is still useful without access to the source tree.
val bundledDocumentationSources = listOf(
    "business-glossary-de.md",
    "hs-office-data-structure.md",
    "hs-hosting-asset-type-structure.md",
    "projects-booking-items-and-hosting-entities.md"
)

val bundledDocumentationMarkdownDir = layout.buildDirectory.dir("doc-bundle/markdown")
val bundledDocumentationHtmlDir = layout.buildDirectory.dir("doc-bundle/html")
val bundledDocumentationIndexDir = layout.buildDirectory.dir("doc-bundle/index")
val bundledApiHtmlDir = layout.buildDirectory.dir("doc-bundle/api")
val bundledDocumentationArchiveDir = layout.buildDirectory.dir("doc-bundle/archive")
val bundledDocumentationArchiveName = "hsadmin-ng-documentation.zip"
val bundledDocumentationStylesheet = layout.projectDirectory.file("doc/.documentation.css")
val markdownToHtmlTool = layout.projectDirectory.file("tools/markdown-to-html")
val bundledDocumentationPageFooter = layout.projectDirectory.file("doc/.documentation-footer.html")
val bundledDocumentationLogo = layout.projectDirectory.file("doc/.hostsharing-logo.svg")

// The rendering needs pandoc plus the diagram filters, thus it is opt-in via `bootJarWithDocumentation`.
val renderedDocumentationRequested = project.hasProperty("includeRenderedDocumentation")
val documentationLinkExtension = if (renderedDocumentationRequested) "html" else "md"

// Scenario report file names carry the report title including ": ", which does not belong into an URL.
fun scenarioReportSlug(markdownFileName: String) =
    markdownFileName
        .removeSuffix(".md")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

// Scenario reports cross-link each other by their original file name, e.g.
// `[...](<1010: Create Legal Person As Partner.md>)`, which the slug has to follow.
val scenarioReportLinkPattern = Regex("""\]\(<([^>]+\.md)>\)""")

fun rewriteScenarioReportLinks(line: String, linkExtension: String) =
    scenarioReportLinkPattern.replace(line) { match ->
        "](${scenarioReportSlug(match.groupValues[1])}.$linkExtension)"
    }

// A leading heading is only the document title if nothing but the optional YAML frontmatter
// precedes it; several documents start with prose and use their first heading for a section.
fun markdownTitle(markdownFile: File): String {
    val lines = markdownFile.readLines().dropWhile { it.isBlank() }
    val body = if (lines.firstOrNull()?.trim() == "---")
        lines.drop(1).dropWhile { it.trim() != "---" }.drop(1)
    else lines
    return body.firstOrNull { it.isNotBlank() }
        ?.takeIf { it.startsWith("#") }
        ?.trimStart('#')?.trim()
        // emphasis and code markers would show up literally in a page title or an index entry
        ?.replace(Regex("""[*`]"""), "")
        ?.takeIf { it.isNotBlank() }
        ?: markdownFile.nameWithoutExtension
}

fun htmlEscaped(text: String) =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

// stroke-based, so that they take the colour of the text they sit in
fun documentationIcon(cssClass: String, path: String) =
    """<svg class="$cssClass" viewBox="0 0 16 16" aria-hidden="true"><path d="$path" fill="none" """ +
            """stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>"""

val downloadIcon = documentationIcon("doc-download-icon", "M8 1v8M4.5 6.5 8 10l3.5-3.5M2 13h12")
val previousIcon = documentationIcon("doc-nav-icon", "M10 3 5 8l5 5")
val nextIcon = documentationIcon("doc-nav-icon", "M6 3l5 5-5 5")
val newTabIcon = documentationIcon("doc-newtab-icon", "M7 3.5H3.5v9h9V9M10 3.5h2.5V6M12.5 3.5 8 8")

// These targets bring no navigation of their own, thus they open beside the documentation.
fun newTabLink(href: String, label: String, reason: String) =
    """<a href="$href" target="_blank" rel="noopener" title="${htmlEscaped(reason)}">""" +
            """$label <span class="doc-newtab">$newTabIcon</span></a>"""

// The overview carries the title as its own heading, a rendered document brings its own, so there
// the title has to stay out of the heading outline.
val bundledDocumentationLogoName = "hostsharing-logo.svg"

// Pandoc would inline a reachable image into every one of the rendered pages, thus the header
// carries a marker which gets replaced by the reference once Pandoc is done.
val documentationLogoMarker = "<!--doc-logo-->"

fun documentationLogo(href: String) =
    """<img class="doc-logo-image" src="$href" alt="Hostsharing eG" width="148" height="24">"""

fun documentationHeader(titleTag: String, logo: String) = buildString {
    appendLine("<header class=\"doc-header\">")
    appendLine("  <a class=\"doc-logo\" href=\"https://www.hostsharing.net\">")
    appendLine("    $logo")
    appendLine("  </a>")
    appendLine("  <div>")
    appendLine("    <$titleTag class=\"doc-header-title\">hsadmin-ng Documentation</$titleTag>")
    appendLine("    <p class=\"doc-eyebrow\">bundled with this build of the application</p>")
    appendLine("  </div>")
    appendLine("</header>")
}

// A single document lists its entries as level-2 headings, e.g. one per release note.
fun entryCount(markdownDir: File, section: BundledDocumentationSection) =
    File(markdownDir, "${section.directory}/${section.singleDocument}.md")
        .takeIf { it.isFile }
        ?.readLines()
        ?.count { it.startsWith("## ") }
        ?: 0

// Readers have to see at a glance how far they can trust a part. The legend keeps every entry
// terse, so that all of them fit on one line below the cards.
// A section of a single document links to it directly, instead of to a list of one entry.
class BundledDocumentationSection(
    val directory: String,
    val heading: String,
    val icon: String,
    val origin: String,
    val legend: String,
    val singleDocument: String? = null,
    // a section of a single document counts the entries of that document instead of its files
    val entryNoun: String? = null
)

val bundledDocumentationSections = listOf(
    BundledDocumentationSection(
        "news", "News", "📣",
        "Written by hand for each change, thus every entry states what changed at its date.",
        legend = "dated per change",
        singleDocument = "release-notes",
        entryNoun = "release notes"
    ),
    BundledDocumentationSection(
        "business", "Business Documentation", "✍️",
        "Written by hand, thus it can be out of date with respect to the code.",
        legend = "can drift from the code"
    ),
    BundledDocumentationSection(
        "rbac-concept", "RBAC Concept", "✍️",
        "Written by hand, thus it can be out of date with respect to the code.",
        legend = "can drift from the code",
        singleDocument = "rbac"
    ),
    BundledDocumentationSection(
        "rbac", "RBAC Diagrams", "⚙️",
        "Generated from the RBAC specifications in the entity sources, thus it matches this build.",
        legend = "generated from code"
    ),
    BundledDocumentationSection(
        "scenarios", "Scenario Reports", "🧪",
        "Generated by running the scenario tests against the API, thus it matches this build.",
        legend = "generated by tests"
    ),
    BundledDocumentationSection(
        "api", "API Documentation", "📐",
        "Written by hand, but it is the source the controller interfaces are generated from, " +
                "thus the signatures match this build.",
        legend = "specification / generation source"
    )
)

// test.yaml is left out on purpose: it only describes the endpoints of the rbactest entities.
class BundledApiSpec(val rootSpec: String, val name: String, val title: String)

val bundledApiSpecs = listOf(
    BundledApiSpec("hs-office/hs-office.yaml", "hs-office", "HS Office API"),
    BundledApiSpec("rbac/rbac.yaml", "rbac", "RBAC API"),
    BundledApiSpec("hs-booking/hs-booking.yaml", "hs-booking", "HS Booking API"),
    BundledApiSpec("hs-hosting/hs-hosting.yaml", "hs-hosting", "HS Hosting API"),
    BundledApiSpec("accounts/api-paths.yaml", "accounts", "HS Accounts API"),
    BundledApiSpec("api-definition.yaml", "root", "Root API")
)

val apiDefinitionDir = layout.projectDirectory.dir("src/main/resources/api-definition")
val openApiToHtmlTool = layout.projectDirectory.file("tools/openapi-to-html")

val pullRequestDocumentationDir = layout.projectDirectory.dir("doc/PR")
val releaseNotesDir = layout.buildDirectory.dir("doc-bundle/release-notes")

// Written once, where the change is explained anyway, instead of in a changelog which gets forgotten.
val generateReleaseNotes = tasks.register("generateReleaseNotes") {
    description = "Collects the release notes of all pull-request documents into the news document."
    group = "Conversion"

    inputs.dir(pullRequestDocumentationDir).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(releaseNotesDir)

    doLast {
        val datedPullRequest = Regex("""^(\d{4}-\d{2}-\d{2})-PR#-?\d+-""")
        val releasedPattern = Regex("""^released:\s*(\d{4}-\d{2}-\d{2})\s*$""", RegexOption.MULTILINE)

        val entries = pullRequestDocumentationDir.asFile
            .listFiles { file -> file.isFile && file.extension == "md" }
            .orEmpty()
            .mapNotNull { file ->
                val startedOn = datedPullRequest.find(file.name)?.groupValues?.get(1) ?: return@mapNotNull null
                val appendix = file.readText()
                    .substringAfter("\n## Release Note\n", "")
                    .substringBefore("\n## ")
                    .trim()
                if (appendix.isEmpty()) return@mapNotNull null

                // The appendix carries its own headline, because the PR title is written for
                // reviewers and may even be in another language than this public page.
                val headline = appendix.lineSequence()
                    .firstOrNull { it.startsWith("### ") }
                    ?.removePrefix("### ")?.trim()
                    ?: markdownTitle(file).replace(Regex("""Taiga#\d+:\s*"""), "")
                val note = appendix.substringAfter("### $headline", appendix).trim()

                // a PR is merged long after its document was started, thus the note names the day
                // it went live; the document's own date only serves as a fallback
                val releasedOn = releasedPattern.find(appendix)?.groupValues?.get(1) ?: startedOn

                if (note.isEmpty()) null else Triple(releasedOn, headline, note)
            }
            .sortedByDescending { (date, _, _) -> date }

        val newsFile = File(releaseNotesDir.get().asFile, "release-notes.md")
        newsFile.parentFile.mkdirs()
        newsFile.writeText(buildString {
            appendLine("# News")
            appendLine()
            appendLine("What changed for whoever uses the REST API, newest first.")
            appendLine("Every entry states what changed at its date; it does not describe the current state.")
            entries.forEach { (date, title, note) ->
                appendLine()
                appendLine("## $date \u2014 $title")
                appendLine()
                appendLine(note)
            }
        })
        logger.lifecycle("Collected ${entries.size} release notes into the news document.")
    }
}

val collectBundledDocumentation = tasks.register<Sync>("collectBundledDocumentation") {
    description = "Collects the Markdown documentation which gets bundled into the application jar."
    group = "Conversion"

    into(bundledDocumentationMarkdownDir)
    includeEmptyDirs = false

    from(generateReleaseNotes) {
        eachFile { path = "news/$name" }
    }
    from(layout.projectDirectory.dir("doc")) {
        include(bundledDocumentationSources)
        eachFile { path = "business/$name" }
    }
    from(layout.projectDirectory.file("doc/rbac.md")) {
        eachFile { path = "rbac-concept/$name" }
    }
    from(bundledDocumentationLogo) {
        eachFile { path = bundledDocumentationLogoName }
    }
    from(layout.projectDirectory.dir("src/main/resources/db/changelog")) {
        include("**/*-rbac*.md")
        exclude("2-rbactest/**") // the rbactest entities only carry test data
        eachFile { path = "rbac/$name" }
    }
    inputs.property("documentationLinkExtension", documentationLinkExtension)

    from(scenarioReportMarkdownDir) {
        include("*.md")
        exclude(".*") // the scenario tests also drop .last-debug-log.md here
        filter { line -> rewriteScenarioReportLinks(line, documentationLinkExtension) }
        eachFile { path = "scenarios/${scenarioReportSlug(name)}.md" }
    }

    doLast {
        if (!scenarioReportMarkdownDir.get().asFile.exists()) {
            logger.warn(
                "No scenario reports bundled - run './gradlew scenarioTest' or './gradlew bootJarWithDocumentation'."
            )
        }
    }
}

tasks.register("renderBundledDocumentation") {
    description = "Renders the bundled Markdown to self-contained HTML with embedded Mermaid and PlantUML diagrams."
    group = "Conversion"

    dependsOn(collectBundledDocumentation)

    inputs.dir(bundledDocumentationMarkdownDir).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(markdownToHtmlTool).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(bundledDocumentationStylesheet).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(bundledDocumentationPageFooter).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(bundledDocumentationLogo).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(bundledDocumentationHtmlDir)

    doLast {
        val markdownDir = bundledDocumentationMarkdownDir.get().asFile
        val htmlDir = bundledDocumentationHtmlDir.get().asFile
        // a renamed or removed source document must not leave its page behind
        htmlDir.deleteRecursively()

        val header = documentationHeader("p", documentationLogoMarker)

        fun neighbourLink(neighbour: File?, label: String) =
            if (neighbour == null) "<span class=\"doc-prevnext-placeholder\">$label</span>"
            else "<a href=\"${neighbour.nameWithoutExtension}.html\"" +
                    " title=\"${htmlEscaped(markdownTitle(neighbour))}\">$label</a>"

        bundledDocumentationSections.forEach { section ->
            val pages = File(markdownDir, section.directory)
                .listFiles { file -> file.isFile && file.extension == "md" }
                .orEmpty()
                .sortedBy { it.name }

            pages.forEachIndexed { index, markdownFile ->
                val navFile = File(temporaryDir, "nav-${section.directory}-${markdownFile.name}.html")
                // a section of a single document has neither a list page to point a crumb at,
                // nor neighbours to step to
                val neighbours = if (pages.size < 2) "" else
                    neighbourLink(pages.getOrNull(index - 1), "$previousIcon Previous") +
                            "\n                            " +
                            neighbourLink(pages.getOrNull(index + 1), "Next $nextIcon")

                val sectionCrumb = if (section.singleDocument != null) "" else
                    """
                            <span class="doc-crumb-separator">&rsaquo;</span>
                            <a href="../${section.directory}.html">${htmlEscaped(section.heading)}</a>"""

                navFile.writeText(
                    header + """
                    <nav class="doc-pagenav">
                        <span class="doc-crumbs">
                            <a href="../index.html">hsadmin-ng Documentation</a>$sectionCrumb
                        </span>
                        <span class="doc-prevnext">$neighbours</span>
                    </nav>
                    """.trimIndent()
                )

                val targetFile = File(htmlDir, "${section.directory}/${markdownFile.nameWithoutExtension}.html")
                val proc2 = ProcessBuilder(
                    markdownToHtmlTool.asFile.absolutePath,
                    markdownFile.absolutePath,
                    targetFile.absolutePath,
                    bundledDocumentationStylesheet.asFile.absolutePath,
                    navFile.absolutePath,
                    bundledDocumentationPageFooter.asFile.absolutePath
                ).inheritIO().start()
                proc2.waitFor()
                if (proc2.exitValue() != 0) throw GradleException("markdownToHtml failed for ${markdownFile.name}")
                // every rendered document sits one level below the logo in the documentation tree
                targetFile.writeText(
                    targetFile.readText().replace(
                        documentationLogoMarker,
                        documentationLogo("../$bundledDocumentationLogoName")
                    )
                )
            }
        }
    }
}

tasks.register("renderBundledApiDocumentation") {
    description = "Renders the OpenAPI definitions to Redoc pages for the documentation bundled into the jar."
    group = "Conversion"

    inputs.dir(apiDefinitionDir).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(openApiToHtmlTool).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(bundledApiHtmlDir)

    doLast {
        val targetDir = bundledApiHtmlDir.get().asFile
        // a renamed or removed spec must not leave its page behind
        targetDir.deleteRecursively()

        ProcessBuilder(openApiToHtmlTool.asFile.absolutePath, "--install-redoc", targetDir.absolutePath)
            .inheritIO().start().waitFor()
        bundledApiSpecs.forEach { spec ->
            val pr = ProcessBuilder(
                openApiToHtmlTool.asFile.absolutePath,
                apiDefinitionDir.file(spec.rootSpec).asFile.absolutePath,
                targetDir.absolutePath,
                spec.name,
                spec.title
            ).inheritIO().start()
            pr.waitFor()
            if (pr.exitValue() != 0) throw GradleException("openApiToHtml failed for ${spec.name}")
        }
    }
}

fun documentationPage(title: String, stylesheet: String, footer: String, body: String) = buildString {
    appendLine("<!doctype html>")
    appendLine("<html lang=\"en\">")
    appendLine("<head>")
    appendLine("  <meta charset=\"utf-8\">")
    appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
    appendLine("  <title>${htmlEscaped(title)}</title>")
    appendLine("  <style>")
    appendLine(stylesheet.prependIndent("    "))
    appendLine("  </style>")
    appendLine("</head>")
    appendLine("<body>")
    append(body)
    appendLine(footer.trim().prependIndent("  "))
    appendLine("</body>")
    appendLine("</html>")
}

tasks.register("generateBundledDocumentationIndex") {
    description = "Generates the overview page and the per-section pages of the bundled documentation."
    group = "Conversion"

    dependsOn(collectBundledDocumentation)
    if (renderedDocumentationRequested) {
        dependsOn(tasks.named("renderBundledDocumentation"))
        dependsOn(tasks.named("renderBundledApiDocumentation"))
    }

    inputs.dir(bundledDocumentationMarkdownDir).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(bundledDocumentationStylesheet).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(bundledDocumentationLogo).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(bundledDocumentationPageFooter).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(bundledDocumentationIndexDir)

    doLast {
        val markdownDir = bundledDocumentationMarkdownDir.get().asFile
        val stylesheet = bundledDocumentationStylesheet.asFile.readText()
        val footer = bundledDocumentationPageFooter.asFile.readText()
        val targetDir = bundledDocumentationIndexDir.get().asFile
        targetDir.deleteRecursively()
        targetDir.mkdirs()

        // Swagger UI reads the API definitions directly, thus the API section is never empty,
        // even when the static pages were not rendered.
        fun entriesOf(section: BundledDocumentationSection): List<String> =
            if (section.directory == "api") {
                buildList {
                    add(
                        newTabLink(
                            "/swagger-ui/index.html", "Swagger UI",
                            "opens in a new tab, because Swagger UI brings its own navigation"
                        ) + " <span class=\"doc-hint\">interactive, needs the running application</span>"
                    )
                    if (renderedDocumentationRequested) {
                        bundledApiSpecs.forEach {
                            add(
                                newTabLink(
                                    "api/${it.name}.html", htmlEscaped(it.title),
                                    "opens in a new tab, because Redoc takes over the whole page"
                                ) + " " + newTabLink(
                                    "api/${it.name}.json",
                                    "<span class=\"doc-hint\">OpenAPI spec</span>",
                                    "opens in a new tab, because it is the raw specification"
                                )
                            )
                        }
                    }
                }
            } else {
                File(markdownDir, section.directory)
                    .listFiles { file -> file.isFile && file.extension == "md" }
                    .orEmpty()
                    .sortedBy { it.name }
                    .map {
                        val link = "${section.directory}/${it.nameWithoutExtension}.$documentationLinkExtension"
                        "<a href=\"$link\">${htmlEscaped(markdownTitle(it))}</a>"
                    }
            }

        val sectionEntries = bundledDocumentationSections.associateWith { entriesOf(it) }
            .filterValues { it.isNotEmpty() }

        val header = documentationHeader("h1", documentationLogo(bundledDocumentationLogoName))
            .prependIndent("  ")

        File(targetDir, "index.html").writeText(
            documentationPage("hsadmin-ng Documentation", stylesheet, footer, buildString {
                append(header)
                // otherwise this page looks complete, while every document behind it downloads
                // as a file instead of being shown
                if (!renderedDocumentationRequested) {
                    appendLine("  <p class=\"doc-warning\">")
                    appendLine("    This build carries the Markdown sources only, thus the documents download")
                    appendLine("    instead of being shown.")
                    appendLine("    Build with <code>./gradlew bootJarWithDocumentation</code> to get the rendered pages.")
                    appendLine("  </p>")
                }
                appendLine("  <div class=\"doc-cards\">")
                sectionEntries.forEach { (section, entries) ->
                    val target = section.singleDocument
                        ?.let { "${section.directory}/$it.$documentationLinkExtension" }
                        ?: "${section.directory}.html"
                    appendLine("    <a class=\"doc-card\" href=\"$target\">")
                    appendLine("      <span class=\"doc-card-icon\">${section.icon}</span>")
                    appendLine("      <h2>${htmlEscaped(section.heading)}</h2>")
                    val count = when {
                        section.singleDocument == null ->
                            "${entries.size} " + if (entries.size == 1) "document" else "documents"
                        section.entryNoun != null ->
                            "${entryCount(markdownDir, section)} ${section.entryNoun}"
                        else -> null
                    }
                    if (count != null) {
                        appendLine("      <p class=\"doc-card-count\">$count</p>")
                    }
                    appendLine("      <p class=\"doc-card-origin\">${htmlEscaped(section.origin)}</p>")
                    appendLine("    </a>")
                }
                appendLine("  </div>")
                appendLine("  <p class=\"doc-legend\">")
                appendLine(
                    bundledDocumentationSections
                        .distinctBy { it.icon }
                        .joinToString(" &middot; ") { "${it.icon} ${htmlEscaped(it.legend)}" }
                        .prependIndent("    ")
                )
                appendLine("  </p>")
                // absolute, because this page is also inside the archive, where a relative link would dangle
                appendLine("  <p>")
                appendLine(
                    "    <a class=\"doc-download\" href=\"/doc/$bundledDocumentationArchiveName\"" +
                            " title=\"Download this documentation as a ZIP," +
                            " to read it without the running application\">"
                )
                appendLine("      $downloadIcon Download this documentation")
                appendLine("    </a>")
                appendLine("  </p>")
            })
        )

        sectionEntries.filterKeys { it.singleDocument == null }.forEach { (section, entries) ->
            File(targetDir, "${section.directory}.html").writeText(
                documentationPage(section.heading, stylesheet, footer, buildString {
                    append(header)
                    appendLine("  <nav class=\"doc-pagenav\">")
                    appendLine("    <span class=\"doc-crumbs\">")
                    appendLine("      <a href=\"index.html\">hsadmin-ng Documentation</a>")
                    appendLine("      <span class=\"doc-crumb-separator\">&rsaquo;</span>")
                    appendLine("      <span>${htmlEscaped(section.heading)}</span>")
                    appendLine("    </span>")
                    appendLine("  </nav>")
                    appendLine("  <h2>${section.icon} ${htmlEscaped(section.heading)}</h2>")
                    appendLine("  <p class=\"doc-card-origin\">${htmlEscaped(section.origin)}</p>")
                    appendLine("  <ul class=\"doc-list\">")
                    entries.forEach { appendLine("    <li>$it</li>") }
                    appendLine("  </ul>")
                })
            )
        }
    }
}

// so that readers can take the documentation with them instead of extracting it from the jar
val archiveBundledDocumentation = tasks.register<Zip>("archiveBundledDocumentation") {
    description = "Packs the bundled documentation into a ZIP offered for download below /doc/."
    group = "Conversion"

    archiveFileName.set(bundledDocumentationArchiveName)
    destinationDirectory.set(bundledDocumentationArchiveDir)

    from(collectBundledDocumentation)
    from(tasks.named("generateBundledDocumentationIndex"))
    if (renderedDocumentationRequested) {
        from(tasks.named("renderBundledDocumentation"))
        from(tasks.named("renderBundledApiDocumentation")) { into("api") }
    }
}

// as regular resources, so that `bootRun` and the tests serve the same documentation as the jar
tasks.named<Copy>("processResources") {
    from(collectBundledDocumentation) { into("static/doc") }
    from(tasks.named("generateBundledDocumentationIndex")) { into("static/doc") }
    from(archiveBundledDocumentation) { into("static/doc") }
    if (renderedDocumentationRequested) {
        from(tasks.named("renderBundledDocumentation")) { into("static/doc") }
        from(tasks.named("renderBundledApiDocumentation")) { into("static/doc/api") }
    }
}

// --- curated documentation PDFs, a build artifact of its own -----------------

val documentationMarkdownSources = listOf(
    "glossary.md",
    "business-glossary-de.md",
    "hs-office-data-structure.md",
    "hs-hosting-asset-type-structure.md",
    "projects-booking-items-and-hosting-entities.md",
    "test-concept.md",
    "rbac.md"
)

val markdownToPdfTool = layout.projectDirectory.file("tools/markdown-to-pdf")
val documentationPdfDir = layout.buildDirectory.dir("doc")

fun markdownPdfTaskSuffix(source: String) =
    source
        .removeSuffix(".md")
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
        .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }

val documentationPdfTasks = documentationMarkdownSources.map { source ->
    val sourceFile = layout.projectDirectory.file("doc/$source")
    val targetFile = documentationPdfDir.map { it.file(source.removeSuffix(".md") + ".pdf") }

    tasks.register<Exec>("convert${markdownPdfTaskSuffix(source)}MarkdownToPdf") {
        description = "Generates ${targetFile.get().asFile} from ${sourceFile.asFile}."
        group = "Conversion"

        inputs.file(sourceFile).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.file(markdownToPdfTool).withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.file(targetFile)
        outputs.cacheIf { true }

        doFirst {
            targetFile.get().asFile.parentFile.mkdirs()
        }

        commandLine(
            markdownToPdfTool.asFile.absolutePath,
            sourceFile.asFile.absolutePath,
            targetFile.get().asFile.absolutePath
        )
    }
}

fun documentationIndexDirs(rootDir: File): Set<File> {
    if (!rootDir.exists()) {
        return emptySet()
    }

    val dirs = mutableSetOf<File>()
    rootDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "pdf" }
        .forEach { pdf ->
            var dir: File? = pdf.parentFile
            while (dir != null && dir.toPath().startsWith(rootDir.toPath())) {
                dirs += dir
                if (dir == rootDir) {
                    break
                }
                dir = dir.parentFile
            }
        }
    return dirs
}

tasks.register("generateDocumentationIndex") {
    description = "Generates recursive HTML indexes linking to generated documentation PDFs."
    group = "Conversion"

    dependsOn(documentationPdfTasks)
    dependsOn(tasks.named("generateScenarioTestIndex"))

    val pdfFiles = fileTree(documentationPdfDir) {
        include("**/*.pdf")
    }

    inputs.files(pdfFiles).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(scenarioReportIndexFile).optional().withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.files(provider {
        documentationIndexDirs(documentationPdfDir.get().asFile)
            .map { File(it, "index.html") }
    })

    doLast {
        val rootDir = documentationPdfDir.get().asFile
        if (!rootDir.exists()) {
            return@doLast
        }

        documentationIndexDirs(rootDir)
            .sortedByDescending { it.toPath().nameCount }
            .forEach { dir ->
                val pdfLinks = dir
                    .listFiles { file -> file.isFile && file.extension == "pdf" }
                    .orEmpty()
                    .sortedBy { it.name }
                    .map { it.name to it.name }

                val indexLinks = dir
                    .listFiles { file -> file.isDirectory && File(file, "index.html").isFile }
                    .orEmpty()
                    .sortedBy { it.name }
                    .map { it.name to "${it.name}/index.html" }

                if (pdfLinks.isEmpty() && indexLinks.isEmpty()) {
                    return@forEach
                }

                File(dir, "index.html").writeText(buildString {
                    appendLine("<!doctype html>")
                    appendLine("<html lang=\"en\">")
                    appendLine("<head>")
                    appendLine("  <meta charset=\"utf-8\">")
                    appendLine("  <title>Documentation PDFs</title>")
                    appendLine("</head>")
                    appendLine("<body>")
                    appendLine("  <h1>Documentation PDFs</h1>")
                    appendLine("  <ul>")
                    pdfLinks.forEach { (label, link) ->
                        appendLine("    <li><a href=\"$link\" target=\"_blank\">$label</a></li>")
                    }
                    indexLinks.forEach { (label, link) ->
                        appendLine("    <li><a href=\"$link\">$label/</a></li>")
                    }
                    appendLine("  </ul>")
                    appendLine("</body>")
                    appendLine("</html>")
                })
            }
    }
}

tasks.register("generateDocumentation") {
    description = "Generates curated documentation PDFs and an HTML index from Markdown files."
    group = "Conversion"

    dependsOn(documentationPdfTasks)
    dependsOn(tasks.named("generateDocumentationIndex"))
}
