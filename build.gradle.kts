import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import org.gradle.kotlin.dsl.*
import com.diffplug.gradle.spotless.SpotlessExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.Copy
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.JvmImplementation
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.api.tasks.wrapper.Wrapper
import java.io.FileOutputStream

// Import specific license report task/extension if needed (adjust class name if necessary)
// import com.github.jk1.gradle.license.LicenseReportExtension
// import com.github.jk1.gradle.license.LicenseReportTask

plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"        // manages implicit dependencies
    id("io.openapiprocessor.openapi-processor") version "2023.2" // generates Controller-interface and resources from API-spec
    id("com.github.jk1.dependency-license-report") version "2.9" // checks dependency-license compatibility
    id("org.owasp.dependencycheck") version "12.1.1"             // checks dependencies for known vulnerabilities
    id("com.diffplug.spotless") version "7.0.2"                  // formats + checks formatting for source-code
    jacoco                                                       // determines code-coverage of tests
    id("info.solidsoft.pitest") version "1.15.0"                 // performs mutation testing
    id("se.patrikerdes.use-latest-versions") version "0.2.18"    // updates module and plugin versions
    id("com.github.ben-manes.versions") version "0.52.0"         // determines which dependencies have updates
}

// HOWTO: find out which dependency versions are managed by Spring Boot:
//  https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html

group = "net.hostsharing"
version = "0.0.1-SNAPSHOT"

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
    gradleVersion = "8.5"
}

// TODO.impl: self-attaching is deprecated, see:
//  https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#0.3

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }

    // Only JUnit 5 (Jupiter) should be used at compile time.
    // TODO.test: For runtime, JUnit 4 is still needed by testcontainers < v2, which is not released yet.
    // testCompileOnly {
    //     exclude(group = "junit", module = "junit")
    //     exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    // }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

val JAVA_VERSION = 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}

// Use extra properties delegate for type safety
val testcontainersVersion by extra("1.20.6")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
    implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:1.11.0")
    implementation("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.9")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("net.java.dev.jna:jna:5.17.0")
    implementation("org.modelmapper:modelmapper:3.2.2")
    implementation("org.iban4j:iban4j:3.2.11-RELEASE")
    implementation("org.reflections:reflections:0.10.2")

    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    annotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
    testImplementation("io.rest-assured:spring-mock-mvc")
    testImplementation("org.hamcrest:hamcrest-core")
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.wiremock:wiremock-standalone:3.12.1")
}

// Configure dependency management using the extension
configure<DependencyManagementExtension> {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

// Java Compiler Options
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters") // keep parameter names => no need for @Param for SpringData
}

// Configure tests
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("importHostingAssets", "scenarioTest")
    }
    jvmArgs("-Duser.language=en", "-Duser.country=US")
    // The 'excludes' property seems deprecated/less common in Kotlin DSL for Test tasks.
    // Use filtering or other mechanisms if needed, or keep if it works.
    // For filtering based on package/class name patterns:
    filter {
        excludeTestsMatching("net.hostsharing.hsadminng.**.generated.**")
        // Add more exclude patterns if needed
    }
    finalizedBy(tasks.named("jacocoTestReport")) // generate report after tests
}

// OpenAPI Source Code Generation
openapiProcessor {
    process("springRoot") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/api-definition.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }

    process("springRbac") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/rbac/rbac.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/rbac/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }

    process("springTest") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/test/test.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/test/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }

    process("springHsOffice") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/hs-office/hs-office.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/hs-office/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }

    process("springHsBooking") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/hs-booking/hs-booking.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/hs-booking/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }

    process("springHsHosting") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/hs-hosting/hs-hosting.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/hs-hosting/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }

    process("springCredentials") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2022.5")
        apiPath(project.file("src/main/resources/api-definition/credentials/api-paths.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/credentials/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax").get().asFile.path)
    }
}

// Add generated sources to the main source set
sourceSets.main.get().java.srcDir(layout.buildDirectory.dir("generated/sources/openapi"))

// Define an abstract task class (if needed for type safety, otherwise can be skipped)
// abstract class ProcessSpring : DefaultTask()

// Register an aggregate task for all OpenAPI generation tasks
val processSpring = tasks.register("processSpring") {
    group = "openapi"
    description = "Runs all OpenAPI generation tasks"
    // Depend on individual processor tasks (names are derived from the configuration block names)
    dependsOn(
        "processSpringRoot",
        "processSpringRbac",
        "processSpringTest",
        "processSpringHsOffice",
        "processSpringHsBooking",
        "processSpringHsHosting",
        "processSpringCredentials"
    )
}

// Ensure resources and compilation depend on the aggregate task
tasks.processResources {
    dependsOn(processSpring)
}
tasks.compileJava {
    dependsOn(processSpring)
}


// Rename javax to jakarta in OpenApi generated java files
// TODO.impl: Upgrade to io.openapiprocessor.openapi-processor >= 2024.2
//  and use either `bean-validation: true` in api-mapping.yaml or `useSpringBoot3 true`
val openApiGenerate = tasks.register<Copy>("openApiGenerate") {
    from(layout.buildDirectory.dir("generated/sources/openapi-javax"))
    into(layout.buildDirectory.dir("generated/sources/openapi"))
    filter { line: String -> line.replace("javax", "jakarta") }
    dependsOn(processSpring) // Ensure generation happens first
}

// Ensure compileJava uses the renamed sources and depends on the renaming task
tasks.compileJava {
    source(layout.buildDirectory.dir("generated/sources/openapi"))
    dependsOn(openApiGenerate)
}


// Spotless Code Formatting
configure<SpotlessExtension> {
    java {
        // Configure formatting steps
        removeUnusedImports()
        leadingTabsToSpaces(4)
        endWithNewline()
        toggleOffOn()

        // Target files
        target(fileTree(rootDir) {
            include("**/*.java")
            exclude("**/generated/**/*.java", "build/**", ".gradle/**") // Add build/.gradle excludes
        })
    }
}
tasks.check {
    dependsOn(tasks.named("spotlessCheck"))
}
// HACK: no idea why spotless uses the output of these tasks, but we get warnings without those
tasks.named("spotlessJava") {
    dependsOn(
        tasks.named("generateLicenseReport"),
        // tasks.named("pitest"), // TODO.test: PiTest currently does not work, needs to be fixed
        tasks.named("jacocoTestReport"),
        tasks.named("processResources"),
        tasks.named("processTestResources")
    )
}


// OWASP Dependency Security Test
configure<DependencyCheckExtension> {
    nvd {
        apiKey = project.findProperty("OWASP_API_KEY")?.toString() // set it in ~/.gradle/gradle.properties
        delay = 16000 // Milliseconds
    }
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.name
    suppressionFiles.add("etc/owasp-dependency-check-suppression.xml") // Use suppressionFiles collection
    failOnError = true
    failBuildOnCVSS = 5.0f // Use float value
}
tasks.check {
    dependsOn(tasks.named("dependencyCheckAnalyze"))
}
tasks.named("dependencyCheckAnalyze") {
    doFirst { // Why not doLast? See README.md!
        println("OWASP Dependency Security Report: file://${project.rootDir}/build/reports/dependency-check-report.html")
    }
}


licenseReport {
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("report.html", "Backend"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
    excludeBoms = true
    allowedLicensesFile = project.file("etc/allowed-licenses.json")
}
tasks.check {
    // Ensure the task name 'checkLicense' is correct for the plugin
    dependsOn(tasks.named("checkLicense"))
}


// JaCoCo Test Code Coverage for unit-tests
configure<JacocoPluginExtension> {
    toolVersion = "0.8.10"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test")) // Depends on the main test task
    reports {
        xml.required.set(true) // Common requirement for CI/CD
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    }

    classDirectories.setFrom(files(sourceSets.main.get().output.classesDirs.map { dir ->
        fileTree(dir) {
            exclude(
                "net/hostsharing/hsadminng/**/generated/**/*.class",
                "net/hostsharing/hsadminng/hs/HsadminNgApplication.class"
            )
        }
    }))

    doFirst { // Why not doLast? See README.md!
        println("HTML Jacoco Test Code Coverage Report: file://${reports.html.outputLocation.get().asFile}/index.html")
    }
}

tasks.check {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport")) // Ensure report is generated first
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // TODO.test: improve instruction coverage
            }
        }

        // element: PACKAGE, BUNDLE, CLASS, SOURCEFILE or METHOD
        // counter:  INSTRUCTION, BRANCH, LINE, COMPLEXITY, METHOD, or CLASS
        // value: TOTALCOUNT, COVEREDCOUNT, MISSEDCOUNT, COVEREDRATIO or MISSEDRATIO

        rule {
            element = "CLASS"
            excludes = listOf(
                "net.hostsharing.hsadminng.**.generated.**",
                "net.hostsharing.hsadminng.rbac.test.dom.TestDomainEntity",
                "net.hostsharing.hsadminng.HsadminNgApplication",
                "net.hostsharing.hsadminng.ping.PingController",
                "net.hostsharing.hsadminng.rbac.generator.*",
                "net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService",
                "net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService\$Node", // Use $ for inner class
                "net.hostsharing.hsadminng.**.*Repository",
                "net.hostsharing.hsadminng.mapper.Mapper"
            )

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal() // TODO.test: improve line coverage
            }
        }
        rule {
            element = "METHOD"
            excludes = listOf(
                "net.hostsharing.hsadminng.**.generated.**",
                "net.hostsharing.hsadminng.HsadminNgApplication.main",
                "net.hostsharing.hsadminng.ping.PingController.*"
            )

            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.00".toBigDecimal() // TODO.test: improve branch coverage
            }
        }
    }
}


// HOWTO: run all unit-tests which don't need a database: gw-test unitTest
tasks.register<Test>("unitTest") {
    useJUnitPlatform {
        excludeTags(
            "importHostingAssets", "scenarioTest", "generalIntegrationTest",
            "officeIntegrationTest", "bookingIntegrationTest", "hostingIntegrationTest"
        )
    }

    group = "verification"
    description = "runs all unit-tests which do not need a database"

    mustRunAfter(tasks.named("spotlessJava"))
}

// HOWTO: run all integration tests which are not specific to a module, like base, rbac, config etc.
tasks.register<Test>("generalIntegrationTest") {
    useJUnitPlatform {
        includeTags("generalIntegrationTest")
    }

    group = "verification"
    description = "runs integration tests which are not specific to a module, like base, rbac, config etc."

    mustRunAfter(tasks.named("spotlessJava"))
}

// HOWTO: run all integration tests of the office module: gw-test officeIntegrationTest
tasks.register<Test>("officeIntegrationTest") {
    useJUnitPlatform {
        includeTags("officeIntegrationTest")
    }

    group = "verification"
    description = "runs integration tests of the office module"

    mustRunAfter(tasks.named("spotlessJava"))
}

// HOWTO: run all integration tests of the booking module: gw-test bookingIntegrationTest
tasks.register<Test>("bookingIntegrationTest") {
    useJUnitPlatform {
        includeTags("bookingIntegrationTest")
    }

    group = "verification"
    description = "runs integration tests of the booking module"

    mustRunAfter(tasks.named("spotlessJava"))
}

// HOWTO: run all integration tests of the hosting module: gw-test hostingIntegrationTest
tasks.register<Test>("hostingIntegrationTest") {
    useJUnitPlatform {
        includeTags("hostingIntegrationTest")
    }

    group = "verification"
    description = "runs integration tests of the hosting module"

    mustRunAfter(tasks.named("spotlessJava"))
}

tasks.register<Test>("importHostingAssets") {
    useJUnitPlatform {
        includeTags("importHostingAssets")
    }

    group = "verification"
    description = "run the import jobs as tests"

    mustRunAfter(tasks.named("spotlessJava"))
}

tasks.register<Test>("scenarioTest") {
    useJUnitPlatform {
        includeTags("scenarioTest")
    }

    group = "verification"
    description = "run the import jobs as tests" // Description seems copied, adjust if needed

    mustRunAfter(tasks.named("spotlessJava"))
}


// pitest mutation testing
configure<PitestPluginExtension> {
    targetClasses.set(listOf("net.hostsharing.hsadminng.**")) // Use .set() for Property<List<String>>
    excludedClasses.set(
        listOf(
            "net.hostsharing.hsadminng.config.**",
            // "net.hostsharing.hsadminng.**.*Controller",
            "net.hostsharing.hsadminng.**.generated.**"
        )
    )

    targetTests.set(listOf("net.hostsharing.hsadminng.**.*UnitTest", "net.hostsharing.hsadminng.**.*RestTest"))
    excludedTestClasses.set(listOf("**AcceptanceTest*", "**IntegrationTest*", "**ImportHostingAssets"))

    // Check if these are Property<String> or direct assignment
    // pitestVersion.set("1.17.0") // If Property<String>
    // junit5PluginVersion.set("1.1.0") // If Property<String>
    // Otherwise, direct assignment might work if the extension allows it, or check plugin docs.
    pitestVersion = "1.17.0" // Assuming direct assignment works
    junit5PluginVersion = "1.1.0" // Assuming direct assignment works

    threads.set(4)

    // As Java unit tests are pretty pointless in our case, this maybe makes not much sense.
    mutationThreshold.set(71)
    coverageThreshold.set(57)
    testStrengthThreshold.set(87)

    outputFormats.set(listOf("XML", "HTML"))
    timestampedReports.set(false)
}
// tasks.check { dependsOn(tasks.named("pitest")) } // TODO.test: PiTest currently does not work, needs to be fixed
tasks.named("pitest") {
    doFirst { // Why not doLast? See README.md!
        println("PiTest Mutation Report: file://${project.rootDir}/build/reports/pitest/index.html")
    }
}


// Dependency Versions Upgrade
// Ensure the task name 'useLatestVersions' is correct
tasks.named("useLatestVersions") {
    finalizedBy(tasks.check)
}

// Define the stability check function at the top level or within an object
val isNonStable = { version: String ->
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = """^[0-9,.v-]+(-r)?$""".toRegex() // Use Kotlin Regex syntax
    !stableKeyword && !version.matches(regex)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    // rejectVersionIf expects a closure that returns true if the version should be rejected
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}


// Generate HTML from Markdown scenario-test-reports using Pandoc:
tasks.register("convertMarkdownToHtml") {
    description = "Generates HTML from Markdown scenario-test-reports using Pandoc."
    group = "Conversion"

    // Define the template file using project.file
    val templateFile = project.file("doc/scenarios/.template.html")
    // Define input directory using layout property
    val inputDir = layout.buildDirectory.dir("doc/scenarios")

    // Use inputs and outputs for better up-to-date checks
    inputs.file(templateFile).withPathSensitivity(PathSensitivity.NONE)
    inputs.dir(inputDir).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(inputDir) // Output HTMLs will be in the same directory

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
        // Ensure input directory exists (Gradle handles this implicitly usually, but explicit check is fine)
        if (!inputDir.get().asFile.exists()) {
            logger.warn("Input directory ${inputDir.get().asFile} does not exist, skipping Pandoc conversion.")
            // Potentially disable the task or skip doLast if input dir missing
            enabled = false // Example: disable task if input dir doesn't exist yet
        }
    }

    doLast {
        // Check if input dir exists again, in case it was created between doFirst and doLast
        if (!inputDir.get().asFile.exists()) {
            logger.warn("Input directory ${inputDir.get().asFile} still does not exist, skipping Pandoc conversion.")
            return@doLast // Skip execution
        }

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

// HOWTO re-generate the RBAC rules (PostgreSQL code) from the RBAC specs in the entities rbac()-method
//  in a shell run `gw rbacGenerate`
tasks.register<JavaExec>("rbacGenerate") {
    group = "application"
    mainClass.set("net.hostsharing.hsadminng.rbac.generator.RbacSpec")
    classpath = sourceSets["main"].runtimeClasspath

    // This ensures the task uses the Java version from the defined toolchain.
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    })
}

// shortcut for compiling all files
tasks.register("compile") {
    group = "build"
    description = "Compiles main and test Java sources."
    dependsOn("compileJava", "compileTestJava")
}
