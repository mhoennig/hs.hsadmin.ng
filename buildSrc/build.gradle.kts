plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.0")
    implementation("com.github.jk1:gradle-license-report:3.1.4")
    implementation("org.owasp:dependency-check-gradle:12.2.2")
}

gradlePlugin {
    plugins {
        register("hsadminTestTasks") {
            id = "hsadmin.test-tasks"
            implementationClass = "HsadminTestTasksPlugin"
        }
    }
}
