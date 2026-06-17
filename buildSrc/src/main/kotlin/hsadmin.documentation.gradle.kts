import java.io.File
import java.io.FileOutputStream

// Generate HTML from Markdown scenario-test-reports using Pandoc:
tasks.register("convertMarkdownToHtml") {
    description = "Generates HTML from Markdown scenario-test-reports using Pandoc."
    group = "Conversion"

    // Define the template file using project.file
    val templateFile = project.file("doc/scenarios/.template.html")
    inputs.file(templateFile).withPathSensitivity(PathSensitivity.NONE)

    // Define input+output directory using layout property
    val inputDir = layout.buildDirectory.dir("doc/scenarios")
    outputs.dir(inputDir) // Output HTMLs will be in the same directory

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
        // Check if pandoc is installed using exec and capturing output/errors
        val result = project.exec {
            commandLine("pandoc", "--version")
            isIgnoreExitValue = true // Don't fail the build immediately
            standardOutput = FileOutputStream(File.createTempFile("pandoc_check", ".out")) // Redirect output
            errorOutput = FileOutputStream(File.createTempFile("pandoc_check", ".err")) // Redirect error
        }
        if (result.exitValue != 0) {
            throw GradleException("Pandoc is not installed or not found in the system path. Please install Pandoc.")
        }

        // Check if the template file exists
        if (!templateFile.exists()) {
            throw GradleException("Template file '$templateFile' not found.")
        }
    }

    doLast {
        // Gather all Markdown files in the input directory
        project.fileTree(inputDir) {
            include("*.md")
        }.forEach { file ->
            // Create the output file path in the same directory
            val outputFile = File(file.parentFile, file.name.replace(".md", ".html"))

            // Execute pandoc for each markdown file
            project.exec {
                commandLine(
                    "pandoc",
                    file.absolutePath,
                    "--template", templateFile.absolutePath,
                    "-o", outputFile.absolutePath
                )
            }
            println("Converted ${file.name} to ${outputFile.name}")
        }
    }
    // Ensure this task runs after scenario tests have potentially generated the markdown files
    dependsOn(tasks.named("scenarioTest"))
}

val scenarioReportDir = layout.buildDirectory.dir("doc/scenarios")
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
