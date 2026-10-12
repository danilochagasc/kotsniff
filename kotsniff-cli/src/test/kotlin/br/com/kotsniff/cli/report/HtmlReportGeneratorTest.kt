package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.TestReports
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class HtmlReportGeneratorTest :
    FunSpec({
        test("escapes untrusted file paths and snippets and stays self-contained") {
            val hostilePath = "weird'path/<x>.kt"
            val hostileSnippet = "</script><script>alert('x')</script> & <b>bold</b>"
            val report =
                TestReports.single(
                    TestReports.diagnostic(path = hostilePath, snippet = hostileSnippet),
                    projectName = "proj<script>",
                )

            val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
            HtmlReportGenerator().generate(report, output, output.parentFile)
            val html = output.readText()

            // The injected script must never survive as markup.
            html shouldNotContain "</script><script>alert"
            html shouldContain "&lt;/script&gt;"
            // The apostrophe in the path is escaped in the data-file attribute.
            html shouldContain "&#39;"
            // No external resources: the report must work offline.
            html shouldNotContain "http://"
            html shouldNotContain "https://"
            html shouldNotContain "src=\"http"
            // Inline script present (filtering).
            html shouldContain "<script>"
        }

        test("renders the severity and rule for a diagnostic") {
            val report = TestReports.single(TestReports.diagnostic())
            val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
            HtmlReportGenerator().generate(report, output, output.parentFile)
            val html = output.readText()

            html shouldContain "Conditional Test Logic"
            html shouldContain "Conditional Test Logic"
            html.contains("medium") shouldBe true
        }

        test("neutralises attribute-breaking paths, unicode and script-like content") {
            val hostilePath = "src/a\"><img src=x onerror=alert(1)>.kt"
            val hostileSnippet = "<script>alert('xss')</script>\u2028\u2029 café ☕ 🎉"
            val report =
                TestReports.single(
                    TestReports.diagnostic(path = hostilePath, snippet = hostileSnippet),
                    projectName = "</title><script>alert(1)</script>",
                )
            val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
            HtmlReportGenerator().generate(report, output, output.parentFile)
            val html = output.readText()

            html shouldNotContain "<img src=x"
            html shouldNotContain "<script>alert"
            html shouldNotContain "</title><script>"
            html shouldContain "&lt;script&gt;"
            html shouldContain "&quot;"
            // Unicode content is preserved (not stripped) but escaped where needed.
            html shouldContain "café"
        }

        test("handles an empty report") {
            val report =
                TestReports.single(TestReports.diagnostic()).copy(
                    smellsFound = 0,
                    smells = emptyList(),
                    smellsByType = emptyMap(),
                    severityCounts = emptyMap(),
                )
            val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
            HtmlReportGenerator().generate(report, output, output.parentFile)
            output.readText() shouldContain "No test smells found"
        }

        test("renders a per-file view with the smell line highlighted") {
            val dir = newProject("Sample.kt" to fiveLineFile())
            try {
                val report = TestReports.single(TestReports.diagnostic(path = "Sample.kt", line = 3))
                val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
                HtmlReportGenerator().generate(report, output, dir)
                val html = output.readText()

                html shouldContain "id=\"file-f0\""
                html shouldContain "class=\"file-btn\""
                html shouldContain "class=\"smell-list\""
                html shouldContain "class=\"smell-link\""
                html shouldContain "class=\"code-wrap\""
                html shouldContain "class=\"line hit\" data-line=\"3\""
                html shouldContain "id=\"prevBtn\""
                html shouldContain "id=\"nextBtn\""
                html shouldContain "class=\"code\""
            } finally {
                deleteRecursively(dir)
            }
        }

        test("escapes hostile content inside source code lines") {
            val hostile = listOf("line1", "line2", "val x = \"</script><b>alert</b>\"", "line4")
            val dir = newProject("Sample.kt" to hostile.joinToString("\n"))
            try {
                val report = TestReports.single(TestReports.diagnostic(path = "Sample.kt", line = 3))
                val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
                HtmlReportGenerator().generate(report, output, dir)
                val html = output.readText()

                html shouldNotContain "</script><b>alert"
                html shouldContain "&lt;/script&gt;&lt;b&gt;alert&lt;/b&gt;"
            } finally {
                deleteRecursively(dir)
            }
        }

        test("windows large files with an omitted-lines marker") {
            val dir = newProject("Big.kt" to (1..50).joinToString("\n") { "line $it" })
            try {
                val report = TestReports.single(TestReports.diagnostic(path = "Big.kt", line = 25))
                val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
                HtmlReportGenerator(maxFileLines = 10).generate(report, output, dir)
                val html = output.readText()

                html shouldContain "lines omitted"
                html shouldContain "class=\"line hit\" data-line=\"25\""
                html shouldNotContain "data-line=\"1\""
            } finally {
                deleteRecursively(dir)
            }
        }

        test("falls back to snippets when the source is unavailable") {
            val report = TestReports.single(TestReports.diagnostic(path = "missing/Nope.kt"))
            val output = File.createTempFile("kotsniff-html", ".html").apply { deleteOnExit() }
            HtmlReportGenerator().generate(report, output, output.parentFile)
            output.readText() shouldContain "source unavailable"
        }
    })

private fun fiveLineFile(): String = (1..5).joinToString("\n") { "line $it" }

private fun newProject(vararg files: Pair<String, String>): File {
    val dir =
        File.createTempFile("kotsniff-htmlproj", "").apply {
            delete()
            mkdirs()
        }
    files.forEach { (name, content) ->
        File(dir, name).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }
    return dir
}

private fun deleteRecursively(file: File) {
    file.listFiles()?.forEach { deleteRecursively(it) }
    file.delete()
}
