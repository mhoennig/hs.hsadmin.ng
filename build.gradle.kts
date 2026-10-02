import org.gradle.kotlin.dsl.*
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.GradleBuild
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.nio.file.Files
import org.apache.tools.ant.util.TeeOutputStream
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.JvmImplementation
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.tasks.wrapper.Wrapper
import java.math.BigDecimal

// Import specific license report task/extension if needed (adjust class name if necessary)
// import com.github.jk1.gradle.license.LicenseReportExtension
// import com.github.jk1.gradle.license.LicenseReportTask

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"        // manages implicit dependencies
    id("com.gorylenko.gradle-git-properties") version "4.0.1"    // exposes git commit info via Actuator info endpoint
    id("io.openapiprocessor.openapi-processor") version "2026.2" // generates Controller-interface and resources from API-spec
    id("info.solidsoft.pitest") version "1.19.0"                 // performs mutation testing
    id("de.javagil.werkcrappit") version "0.5.0"                 // Mutation-CRAP per method, gate mutationCrapCheck
    id("se.patrikerdes.use-latest-versions") version "0.2.19"    // updates module and plugin versions
    id("io.github.ben-manes.versions") version "0.64.0"          // determines which dependencies have updates
}

springBoot {
    buildInfo {
        if (!providers.gradleProperty("includeBuildTime").map(String::toBoolean).getOrElse(false)) {
            excludes.set(setOf("time"))
        }
        properties {
            additional.set(mapOf<String, Any>("host" to InetAddress.getLocalHost().hostName))
        }
    }
}

// HOWTO: find out which dependency versions are managed by Spring Boot:
//  https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html

group = "net.hostsharing"
version = "0.0.1-SNAPSHOT"

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
    gradleVersion = "9.7.1"
}

// TODO.impl: self-attaching is deprecated, see:
//  https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#0.3

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://git.javagil.de/api/packages/mi/maven") } // werkcrappit
}

val JAVA_VERSION = 25

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}

// For special versions, use `extra` properties delegate for type safety
val testcontainersVersion = "2.0.5"
extra["testcontainersVersion"] = testcontainersVersion

val junitVersion = "6.1.3"
extra["junit-jupiter.version"] = junitVersion
extra["lombok.version"] = "1.18.48"

// Spring Boot 4.1.1 pins Tomcat 11.0.24, which is affected by CVE-2026-68763.
extra["tomcat.version"] = "11.0.26"

// Includes JSON-path escaping for CVE-2026-77874.
extra["hibernate.version"] = "7.4.10.Final"

// Align the OpenAPI processor's Kotlin libraries with the CVE-2026-53914 fixed release.
extra["kotlin.version"] = "2.4.20"

// Boot's Jackson versions lack fixes for CVE-2026-89425, CVE-2026-91776 and CVE-2026-91777;
// Jackson 2 is still reachable transitively through swagger-core and datasource-proxy.
extra["jackson-2-bom.version"] = "2.21.7"
extra["jackson-bom.version"] = "3.1.7"

 // Fixes MDC-based path traversal (CVE-2026-19880).
extra["logback.version"] = "1.6.3"

// springdoc resolves swagger-ui resources under the exact webjar version, a mismatch yields 404,
// thus verifySwaggerUiVersionPin (part of `check`) enforces the application.yml pin
val swaggerUiVersion = "5.32.15" // Bundles the DOMPurify fix for CVE-2026-75838.
val verifySwaggerUiVersionPin = tasks.register("verifySwaggerUiVersionPin") {
    val ymlFiles = listOf("src/main/resources/application.yml", "src/test/resources/application.yml").map { file(it) }
    val expectedPin = "version: $swaggerUiVersion"
    inputs.files(ymlFiles)
    inputs.property("expectedPin", expectedPin)
    doLast {
        ymlFiles.forEach { ymlFile ->
            check(ymlFile.readText().contains(expectedPin)) {
                "$ymlFile must contain 'springdoc.swagger-ui.$expectedPin' to match the webjar pinned in build.gradle.kts"
            }
        }
    }
}
tasks.named("check") { dependsOn(verifySwaggerUiVersionPin) }

// Spring Data REST auto-exposes every repository as a CRUD endpoint, outside the API-key and assumed-roles filters
val verifyNoSpringDataRest = tasks.register("verifyNoSpringDataRest") {
    group = "verification"
    description = "Fails if Spring Data REST reappears on the runtime classpath."
    val runtimeJarNames = configurations.runtimeClasspath.map { classpath -> classpath.files.map { it.name } }
    doLast {
        val offending = runtimeJarNames.get().filter { it.startsWith("spring-data-rest") }
        check(offending.isEmpty()) {
            "Spring Data REST is back on the runtime classpath ($offending), which would expose every repository" +
                    " as a CRUD endpoint; remove it again or configure 'spring.data.rest.detection-strategy: annotated'"
        }
    }
}
tasks.named("check") { dependsOn(verifyNoSpringDataRest) }

// JUnit 4 on the test runtime classpath lets @org.junit.Test compile again, and Jupiter then skips those methods silently
val verifyNoJunit4 = tasks.register("verifyNoJunit4") {
    group = "verification"
    description = "Fails if JUnit 4 or the vintage engine reappear on the test runtime classpath."
    val testJarNames = configurations.testRuntimeClasspath.map { classpath -> classpath.files.map { it.name } }
    doLast {
        val offending = testJarNames.get()
            .filter { it.startsWith("junit-4") || it.startsWith("junit-vintage-engine") }
        check(offending.isEmpty()) {
            "JUnit 4 is back on the test runtime classpath ($offending); @org.junit.Test methods would compile" +
                    " again and be skipped silently by the Jupiter engine"
        }
    }
}
tasks.named("check") { dependsOn(verifyNoJunit4) }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
    // springdoc bundles swagger-ui with DOMPurify 3.3.x, which has known sanitizer bypasses
    implementation("org.webjars:swagger-ui:$swaggerUiVersion")
    implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:2.0.1")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("io.hypersistence:hypersistence-utils-hibernate-73:3.16.0")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.openapitools:jackson-databind-nullable:0.2.11")
    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("org.modelmapper:modelmapper:3.2.6")
    implementation("org.iban4j:iban4j:3.2.14-RELEASE")
    implementation("org.reflections:reflections:0.10.2")
    implementation("de.mkammerer:argon2-jvm:2.12")

    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    annotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Boot 4 splits the test slices out of the umbrella starter
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    // Spring Boot 4 no longer manages rest-assured
    testImplementation("io.rest-assured:spring-mock-mvc:6.0.1")
    testImplementation("org.hamcrest:hamcrest-core")
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.3")
    add("pitest", "org.pitest:pitest-history-plugin:0.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
}

// Configure dependency management using the extension
configure<DependencyManagementExtension> {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
    dependencies {
        // TODO.version: remove once the BOM does'nt use the vulnerable verison 3.17.0 anymore
        dependency("org.apache.commons:commons-lang3:3.18.0")
    }
}

// Gradle pins org.junit:junit-bom 5.12.2 in its distribution, which would hold the launcher at 1.x
testing.suites.named<JvmTestSuite>("test") {
    useJUnitJupiter(junitVersion)
}

// Java Compiler Options
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters") // keep parameter names => no need for @Param for SpringData
}

// OpenAPI Source Code Generation
openapiProcessor {
    process("springRoot") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/api-definition.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        // each processor needs its own targetDir, otherwise overlapping outputs break up-to-date checks
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springRoot").get().asFile.path)
    }

    process("springRbac") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/rbac/rbac.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/rbac/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springRbac").get().asFile.path)
    }

    process("springTest") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/test/test.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/test/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springTest").get().asFile.path)
    }

    process("springHsOffice") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/hs-office/hs-office.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/hs-office/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springHsOffice").get().asFile.path)
    }

    process("springHsBooking") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/hs-booking/hs-booking.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/hs-booking/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springHsBooking").get().asFile.path)
    }

    process("springHsHosting") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/hs-hosting/hs-hosting.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/hs-hosting/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springHsHosting").get().asFile.path)
    }

    process("springAccounts") {
        processorName("spring")
        processor("io.openapiprocessor:openapi-processor-spring:2026.3.2")
        apiPath(project.file("src/main/resources/api-definition/accounts/api-paths.yaml").path)
        prop("mapping", project.file("src/main/resources/api-definition/accounts/api-mappings.yaml").path)
        prop("showWarnings", true)
        prop("openApiNullable", true)
        targetDir(layout.buildDirectory.dir("generated/sources/openapi-javax/springAccounts").get().asFile.path)
    }
}

configurations.matching { it.name.endsWith("Classpath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.antlr:antlr4")).using(module("org.antlr:antlr4-runtime:4.13.2"))
            .because("OpenAPI uses pre-generated parsers, not the grammar compiler affected by CVE-2026-13500/13503")
    }
}

// The processor tasks only track their own apiPath/mapping YAML as inputs, but the API definitions
// cross-reference schemas in other module directories (e.g. `$ref: ../rbac/...`). Declare the whole
// api-definition directory as input so changes in referenced files invalidate every generation task.
tasks.matching { it.name.startsWith("processSpring") && it.name != "processSpring" }.configureEach {
    inputs.dir("src/main/resources/api-definition")
        .withPropertyName("apiDefinitionDir")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.cacheIf { true }
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
        "processSpringAccounts"
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
    // copy from each processor's own targetDir, so the process names don't end up in the package paths
    listOf(
        "springRoot", "springRbac", "springTest", "springHsOffice",
        "springHsBooking", "springHsHosting", "springAccounts"
    ).forEach { from(layout.buildDirectory.dir("generated/sources/openapi-javax/$it")) }
    into(layout.buildDirectory.dir("generated/sources/openapi"))
    filter { line: String ->
        line.replace("javax", "jakarta")
            // the @Generated timestamp would defeat build-cache hits for compileJava and downstream
            .replace(Regex("""date = "[^"]+""""), """date = "unset"""")
    }
    dependsOn(processSpring) // Ensure generation happens first
}

// Ensure compileJava uses the renamed sources and depends on the renaming task
tasks.compileJava {
    source(layout.buildDirectory.dir("generated/sources/openapi"))
    dependsOn(openApiGenerate)
}

apply(plugin = "hsadmin.quality")

apply(plugin = "hsadmin.test-tasks")

// pitest mutation testing
configure<PitestPluginExtension> {
    targetClasses.set(listOf("net.hostsharing.hsadminng.**")) // Use .set() for Property<List<String>>
    excludedClasses.set(
        listOf(
            "net.hostsharing.hsadminng.config.**",
            // "net.hostsharing.hsadminng.**.*Controller",
            "net.hostsharing.hsadminng.**.generated.**",
            "net.hostsharing.hsadminng.rbac.generator.**",
            "net.hostsharing.hsadminng.rbac.test.**",
            "net.hostsharing.hsadminng.**.*Repository"
        )
    )

    targetTests.set(listOf("net.hostsharing.hsadminng.**.*UnitTest", "net.hostsharing.hsadminng.**.*RestTest"))
    excludedTestClasses.set(listOf("**AcceptanceTest*", "**IntegrationTest*", "**ImportHostingAssets"))

    pitestVersion = "1.30.0"
    junit5PluginVersion = "1.2.3"

    threads.set(4)
    timeoutFactor.set(BigDecimal("1.5"))
    timeoutConstInMillis.set(8000)
    jvmArgs.set(listOf("-XX:+EnableDynamicAgentLoading", "--enable-native-access=ALL-UNNAMED", "-Xmx2g"))
    // incremental pitest: per-branch history survives `rm -rf build` (worktree-local `.pitest/`)
    // on Gittally each branch has its own worktree, so history is per-branch and compares
    // changed classes only; pitestFull forces full run by deleting history
    enableDefaultIncrementalAnalysis.set(true)
    historyInputLocation.set(layout.projectDirectory.file(".pitest/history.bin"))
    historyOutputLocation.set(layout.projectDirectory.file(".pitest/history.bin"))

    mutationThreshold.set(78)
    coverageThreshold.set(90)
    testStrengthThreshold.set(84)

    outputFormats.set(listOf("XML", "HTML"))
    timestampedReports.set(false)
}
// Mutation-CRAP = C² × (1 − MS)³ + C per method; mutationCrapCheck fails above the limit.
// 10 holds the state of 2026-10-01 after fixing the six methods above it, see doc/PR/*PR#317*.
werkcrappit {
    maxMutationCrap = 10
}

tasks.named("pitest") {
    val reportFile = layout.buildDirectory.file("reports/pitest/index.html")
    doFirst { // Why not doLast? See README.md!
        println("PiTest Mutation Report: ${reportFile.get().asFile.toPath().toUri()}")
    }
}
tasks.register("pitestPartial") {
    group = "verification"
    description = "Runs pitest incremental (only changed classes, history in .pitest/history.bin)"
    dependsOn("pitest")
}
tasks.named("pitest") {
    val fullRunRequested = gradle.startParameter.taskNames.any { it.contains("pitestFull") }
    val historyFile = layout.projectDirectory.file(".pitest/history.bin").asFile
    doFirst {
        if (fullRunRequested) {
            Files.deleteIfExists(historyFile.toPath())
        }
    }
}
tasks.register("pitestFull") {
    group = "verification"
    description = "Runs pitest fully (deletes history, mutates all classes)"
    dependsOn("pitest")
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

apply(plugin = "hsadmin.documentation")

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

fun registerReportingExec(
    name: String,
    taskDescription: String,
    command: List<String>,
    runsAfter: List<String>,
) = tasks.register<Exec>(name) {
    group = "verification"
    description = taskDescription
    // .gitTally appends this task last; keep it last even if the task graph runs in parallel
    mustRunAfter(*runsAfter.toTypedArray())
    commandLine(command)
    // let the report be written before the build fails on a non-zero exit
    isIgnoreExitValue = true
    val reportDir = layout.buildDirectory.dir("reports/$name")
    val captured = ByteArrayOutputStream()
    standardOutput = TeeOutputStream(System.out, captured)
    errorOutput = TeeOutputStream(System.err, captured)
    doFirst {
        reportDir.get().asFile.mkdirs()
    }
    doLast {
        val dir = reportDir.get().asFile
        val output = captured.toString(Charsets.UTF_8)
        val exitValue = executionResult.get().exitValue
        val passed = exitValue == 0
        dir.resolve("output.log").writeText(output)
        val escaped = output.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val statusColor = if (passed) "#4b930c" else "#b60808"
        val statusText = if (passed) "PASSED" else "FAILED"
        dir.resolve("index.html").writeText(
            "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head><meta charset=\"utf-8\"><title>$name report</title></head>\n" +
                "<body>\n" +
                "<h1>$name — <span style=\"color:$statusColor\">$statusText</span> (exit $exitValue)</h1>\n" +
                "<pre style=\"white-space:pre-wrap\">$escaped</pre>\n" +
                "</body>\n" +
                "</html>\n"
        )
        if (!passed) {
            throw GradleException("$name failed with exit code $exitValue; see ${dir.resolve("index.html")}")
        }
    }
}

registerReportingExec(
    "smokeTest",
    "Runs tools/smoke-test: bootRun against a throw-away Docker-PostgreSQL plus dev-tooling checks.",
    listOf("tools/smoke-test"),
    listOf("jacocoTestReport", "check", "generateDocumentation"),
)

registerReportingExec(
    "prQuickCheck",
    "Runs tools/pr-quick-check: inexpensive PR quality gate against the target branch.",
    listOf("tools/pr-quick-check"),
    listOf("jacocoTestReport", "check", "generateDocumentation", "smokeTest"),
)

tasks.register<GradleBuild>("bootJarWithBuildTime") {
    group = "build"
    description = "Builds bootJar with build.time included in META-INF/build-info.properties."

    tasks = listOf("bootJar")
    startParameter.projectProperties = startParameter.projectProperties + ("includeBuildTime" to "true")
}

tasks.register<GradleBuild>("bootJarWithDocumentation") {
    group = "build"
    description = "Builds bootJar with the documentation rendered to HTML; needs pandoc, mermaid-filter and pandoc-plantuml."

    tasks = listOf("scenarioTest", "bootJar")
    startParameter.projectProperties = startParameter.projectProperties + ("includeRenderedDocumentation" to "true")
}


tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // JNA loads libcrypt for the /etc/shadow hashes, which the JDK blocks by default since Java 24
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    if (project.hasProperty("debug")) {
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
    }
}

// Generate git.properties for Spring Boot Actuator /actuator/info
gitProperties {
    // Keep output small but useful for build identification
    keys = listOf(
        "git.commit.id",
        "git.commit.id.abbrev",
        "git.branch",
        "git.commit.time",
        "git.dirty" // true if built from a working tree with uncommitted changes
    )
    // Allow builds from exported sources without .git
    failOnNoGitDirectory = false
}
