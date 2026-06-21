plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    application
}

dependencies {
    implementation(projects.kotsniffCore)
    implementation(projects.kotsniffAnalyzer)

    implementation(libs.clikt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("br.com.kotsniff.cli.MainKt")
    applicationName = "kotsniff"
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    isIgnoreExitValue = true
}

tasks.register("installCli") {
    group = "distribution"
    description = "Installs KotSniff CLI globally on the user's system."
    dependsOn("installDist")

    doLast {
        val osName = System.getProperty("os.name").lowercase()
        val installDistDir = layout.buildDirectory.dir("install/kotsniff").get().asFile.absolutePath
        val scriptsDir = project.projectDir.resolve("scripts")

        val process = if (osName.contains("windows")) {
            val script = scriptsDir.resolve("install.ps1").absolutePath
            ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-File", script, installDistDir)
                .redirectErrorStream(true)
                .start()
        } else {
            val script = scriptsDir.resolve("install.sh").absolutePath
            ProcessBuilder("bash", script, installDistDir)
                .redirectErrorStream(true)
                .start()
        }
        
        process.inputStream.bufferedReader().use { reader ->
            reader.lines().forEach { println(it) }
        }
        
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("Install script failed with exit code $exitCode")
        }
    }
}
