plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.kotsniffCore)
    implementation(libs.kotlin.compiler)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
