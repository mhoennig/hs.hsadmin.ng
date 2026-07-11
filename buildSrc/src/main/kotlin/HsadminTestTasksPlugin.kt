import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class HsadminTestTasksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.withType<Test>().configureEach {
            testLogging {
                events("started", "failed")
            }
        }

        project.tasks.named<Test>("test") {
            useJUnitPlatform {
                includeTags(
                    "migrationTest", "generalIntegrationTest", "officeIntegrationTest",
                    "bookingIntegrationTest", "hostingIntegrationTest"
                )
            }
            jvmArgs("-Duser.language=en", "-Duser.country=US")
            // each fork gets its own Testcontainers PostgreSQL database
            maxParallelForks = System.getenv("HSADMINNG_TEST_MAX_PARALLEL_FORKS")?.toIntOrNull() ?: 3
            filter {
                excludeTestsMatching("net.hostsharing.hsadminng.**.generated.**")
            }
        }

        // Wire custom test tasks to the test source set.
        fun Test.useTestSourceSet() {
            val testSourceSet = project.extensions.getByType<SourceSetContainer>().getByName("test")
            testClassesDirs = testSourceSet.output.classesDirs
            classpath = testSourceSet.runtimeClasspath
        }

        // HOWTO: run all unit-tests - this is useful in an IDE: gw-test anyTest
        project.tasks.register<Test>("anyTest") {
            useTestSourceSet()
            useJUnitPlatform()

            group = "verification"
            description = "runs all unit-tests which do not need a database"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        // HOWTO: run all unit-tests which don't need a database: gw-test unitTest
        project.tasks.register<Test>("unitTest") {
            useTestSourceSet()
            useJUnitPlatform {
                excludeTags(
                    "importHostingAssets", "scenarioTest", "migrationTest", "generalIntegrationTest",
                    "officeIntegrationTest", "bookingIntegrationTest", "hostingIntegrationTest"
                )
            }
            jvmArgs("-Duser.language=en", "-Duser.country=US")

            group = "verification"
            description = "runs all unit-tests which do not need a database"

            mustRunAfter(project.tasks.named("spotlessJava"))
            // no finalizedBy(jacocoTestReport): run `gw jacocoTestReport` explicitly when a report is wanted
        }

        // HOWTO: run all integration tests that are not specific to a module, like base, rbac, config etc.
        project.tasks.register<Test>("generalIntegrationTest") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("generalIntegrationTest")
            }

            group = "verification"
            description = "runs integration tests which are not specific to a module, like base, rbac, config etc."

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        // HOWTO: run all integration tests of the office module: gw-test officeIntegrationTest
        project.tasks.register<Test>("officeIntegrationTest") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("officeIntegrationTest")
            }

            group = "verification"
            description = "runs integration tests of the office module"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        // HOWTO: run all integration tests of the booking module: gw-test bookingIntegrationTest
        project.tasks.register<Test>("bookingIntegrationTest") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("bookingIntegrationTest")
            }

            group = "verification"
            description = "runs integration tests of the booking module"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        // HOWTO: run all integration tests of the hosting module: gw-test hostingIntegrationTest
        project.tasks.register<Test>("hostingIntegrationTest") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("hostingIntegrationTest")
            }

            group = "verification"
            description = "runs integration tests of the hosting module"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        project.tasks.register<Test>("migrationTest") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("migrationTest")
            }

            group = "verification"
            description = "run database migration tests"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        project.tasks.register<Test>("importHostingAssets") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("importHostingAssets")
            }

            group = "verification"
            description = "run the import jobs as tests"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        project.tasks.register<Test>("scenarioTest") {
            useTestSourceSet()
            useJUnitPlatform {
                includeTags("scenarioTest")
            }

            group = "verification"
            description = "run the import jobs as tests"

            mustRunAfter(project.tasks.named("spotlessJava"))
        }

        project.tasks.withType<Test>().configureEach {
            if (name != "unitTest") {
                mustRunAfter(project.tasks.named("unitTest"))
            }
        }
    }
}
