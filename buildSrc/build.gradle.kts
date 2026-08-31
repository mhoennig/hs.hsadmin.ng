plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.0")
    implementation("com.github.jk1:gradle-license-report:2.9")
    implementation("org.owasp:dependency-check-gradle:12.1.3")
}

gradlePlugin {
    plugins {
        register("hsadminTestTasks") {
            id = "hsadmin.test-tasks"
            implementationClass = "HsadminTestTasksPlugin"
        }
    }
}
