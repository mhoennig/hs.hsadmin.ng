import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.ReportRenderer
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension

plugins {
    id("com.github.jk1.dependency-license-report")
    id("org.owasp.dependencycheck")
    id("com.diffplug.spotless")
    jacoco
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
tasks.named("check") {
    dependsOn(tasks.named("spotlessCheck"))
}
// HACK: no idea why spotless uses the output of these tasks, but we get warnings without those
tasks.named("spotlessJava") {
    dependsOn(
        tasks.named("generateLicenseReport"),
        // tasks.named("pitest"), // TODO.test: PiTest currently does not work, needs to be fixed
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
tasks.named("check") {
    dependsOn(tasks.named("dependencyCheckAnalyze"))
}
tasks.named("dependencyCheckAnalyze") {
    doFirst { // Why not doLast? See README.md!
        println("OWASP Dependency Security Report: file://${project.rootDir}/build/reports/dependency-check-report.html")
    }
}


configure<LicenseReportExtension> {
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("report.html", "Backend"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
    excludeBoms = true
    allowedLicensesFile = project.file("etc/allowed-licenses.json")
}
tasks.named("check") {
    // Ensure the task name 'checkLicense' is correct for the plugin
    dependsOn(tasks.named("checkLicense"))
}


// JaCoCo Test Code Coverage for unit-tests
configure<JacocoPluginExtension> {
    toolVersion = "0.8.10"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("unitTest")
    dependsOn(tasks.named("compileJava")) // Add explicit dependency on compileJava
    dependsOn(tasks.named("openApiGenerate")) // Add explicit dependency on openApiGenerate
    mustRunAfter("unitTest") // If unitTest is scheduled, report its coverage data after it ran.

    // Use coverage data from the custom unitTest task instead of the default test task.
    executionData.setFrom(layout.buildDirectory.file("jacoco/unitTest.exec"))

    reports {
        xml.required.set(true) // Common requirement for CI/CD
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    }

    val mainSourceSet = project.extensions.getByType<SourceSetContainer>()["main"]
    classDirectories.setFrom(files(mainSourceSet.output.classesDirs.map { dir ->
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

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("unitTest") // Coverage verification depends only on unit and REST tests.
    dependsOn(tasks.named("jacocoTestReport")) // Ensure report is generated first

    // Use coverage data from the custom unitTest task instead of the default test task.
    executionData.setFrom(layout.buildDirectory.file("jacoco/unitTest.exec"))

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
