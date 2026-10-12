package br.com.kotsniff.cli.runner

import br.com.kotsniff.analyzer.KotSniffEngine
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class KotSniffRunnerTest :
    FunSpec({
        fun newProject(vararg files: Pair<String, String>): File {
            val dir =
                File.createTempFile("kotsniff-runner", "").apply {
                    delete()
                    mkdirs()
                }
            files.forEach { (name, content) ->
                val file = File(dir, name)
                file.parentFile.mkdirs()
                file.writeText(content)
            }
            return dir
        }

        fun deleteRecursively(file: File) {
            file.listFiles()?.forEach { deleteRecursively(it) }
            file.delete()
        }

        val specWithSmell =
            """
            import io.kotest.core.spec.style.StringSpec

            class A : StringSpec({
                "no assertion" {
                    val x = 1
                }
            })
            """.trimIndent()

        test("produces deterministic diagnostics across repeated runs") {
            val dir = newProject("A.kt" to specWithSmell)
            try {
                val runner = KotSniffRunner(KotSniffEngine.createDefault())

                fun signature() =
                    runner
                        .scanDirectory(dir)
                        .diagnostics
                        .map { "${it.ruleId}:${it.location.filePath}:${it.location.line}" }

                signature() shouldBe signature()
            } finally {
                deleteRecursively(dir)
            }
        }

        test("isolates files with syntax errors and reports them") {
            val dir =
                newProject(
                    "Good.kt" to specWithSmell,
                    "Broken.kt" to "class Broken { fun x() { if (true) { println(1) ",
                )
            try {
                val runner = KotSniffRunner(KotSniffEngine.createDefault())
                val summary = runner.scanDirectory(dir)

                summary.failedFiles.map { it.filePath } shouldContain "Broken.kt"
                summary.failedFiles.first { it.filePath == "Broken.kt" }.reason shouldBe "syntax error"
                // Diagnostics only come from the valid file.
                summary.diagnostics.map { it.location.filePath }.toSet() shouldBe setOf("Good.kt")
            } finally {
                deleteRecursively(dir)
            }
        }

        test("invokes the progress callback once per analysed file") {
            val dir = newProject("A.kt" to specWithSmell, "B.kt" to specWithSmell)
            try {
                val runner = KotSniffRunner(KotSniffEngine.createDefault())
                val calls = AtomicInteger(0)
                val lastProcessed = AtomicInteger(0)
                runner.scanDirectory(dir) { processed, total ->
                    calls.incrementAndGet()
                    total shouldBe 2
                    lastProcessed.set(processed)
                }
                calls.get() shouldBe 2
                lastProcessed.get() shouldBe 2
            } finally {
                deleteRecursively(dir)
            }
        }

        test("respects exclusion globs and counts ignored files") {
            val dir = newProject("Keep.kt" to specWithSmell, "Generated.kt" to specWithSmell)
            try {
                val runner = KotSniffRunner(KotSniffEngine.createDefault())
                val summary = runner.scanDirectory(dir, excludePatterns = listOf("**/Generated.kt"))

                summary.totalFilesScanned shouldBe 1
                summary.ignoredFiles shouldBe 1
            } finally {
                deleteRecursively(dir)
            }
        }
    })
