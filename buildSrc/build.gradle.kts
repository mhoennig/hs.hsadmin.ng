plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.2")
    implementation("com.github.jk1:gradle-license-report:2.9")
    implementation("org.owasp:dependency-check-gradle:12.1.1")
}

gradlePlugin {
    plugins {
        register("hsadminTestTasks") {
            id = "hsadmin.test-tasks"
            implementationClass = "HsadminTestTasksPlugin"
        }
    }
}
