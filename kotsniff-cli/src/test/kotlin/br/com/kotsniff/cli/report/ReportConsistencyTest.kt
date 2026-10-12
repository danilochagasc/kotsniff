package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.TestReports
import br.com.kotsniff.core.enums.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Cross-format consistency: every report is produced from the same normalised
 * `Diagnostic` model, so counts and identifiers must agree, and SARIF URIs must
 * use forward slashes even for Windows paths.
 */
class ReportConsistencyTest :
    FunSpec({
        fun tempFile(suffix: String): File = File.createTempFile("kotsniff-report", suffix).apply { deleteOnExit() }

        // A Windows path with backslashes, plus a high-severity override.
        val diagnostic =
            TestReports
                .diagnostic(path = "src\\main\\kotlin\\Windows.kt", snippet = "if (x) y()", line = 4)
                .copy(severity = Severity.HIGH)
        val report = TestReports.single(diagnostic)

        test("counts and rule identifiers agree across formats") {
            val jsonFile = tempFile(".json")
            JsonReportGenerator().generate(report, jsonFile, jsonFile.parentFile)
            val json = Json.parseToJsonElement(jsonFile.readText()).jsonObject
            val jsonSmells = json["smells"]!!.jsonArray
            jsonSmells.size shouldBe 1
            jsonSmells[0].jsonObject["ruleId"]!!.jsonPrimitive.content shouldBe "CONDITIONAL_TEST_LOGIC"
            jsonSmells[0].jsonObject["severity"]!!.jsonPrimitive.content shouldBe "HIGH"

            val sarifFile = tempFile(".sarif")
            SarifReportGenerator().generate(report, sarifFile, sarifFile.parentFile)
            val sarif = Json.parseToJsonElement(sarifFile.readText()).jsonObject
            sarif["version"]!!.jsonPrimitive.content shouldBe "2.1.0"
            val results = sarif["runs"]!!.jsonArray[0].jsonObject["results"]!!.jsonArray
            results.size shouldBe 1
            results[0].jsonObject["ruleId"]!!.jsonPrimitive.content shouldBe "CONDITIONAL_TEST_LOGIC"

            val mdFile = tempFile(".md")
            MarkdownReportGenerator().generate(report, mdFile, mdFile.parentFile)
            mdFile.readText() shouldContain "Conditional Test Logic"

            val txtFile = tempFile(".txt")
            TextReportGenerator().generate(report, txtFile, txtFile.parentFile)
            txtFile.readText() shouldContain "Conditional Test Logic"

            val htmlFile = tempFile(".html")
            HtmlReportGenerator().generate(report, htmlFile, htmlFile.parentFile)
            htmlFile.readText() shouldContain "Conditional Test Logic"
        }

        test("SARIF URIs use forward slashes for Windows paths") {
            val sarifFile = tempFile(".sarif")
            SarifReportGenerator().generate(report, sarifFile, sarifFile.parentFile)
            val sarif = Json.parseToJsonElement(sarifFile.readText()).jsonObject
            val uri =
                sarif["runs"]!!
                    .jsonArray[0]
                    .jsonObject["results"]!!
                    .jsonArray[0]
                    .jsonObject["locations"]!!
                    .jsonArray[0]
                    .jsonObject["physicalLocation"]!!
                    .jsonObject["artifactLocation"]!!
                    .jsonObject["uri"]!!
                    .jsonPrimitive.content
            uri shouldBe "src/main/kotlin/Windows.kt"
            uri shouldNotContain "\\"
        }

        test("HTML escapes the source, message and path for hostile content") {
            val hostile =
                TestReports.diagnostic(
                    path = "src/</script><img src=x>.kt",
                    snippet = "\"</script><b>alert</b>\"",
                )
            val htmlFile = tempFile(".html")
            HtmlReportGenerator().generate(TestReports.single(hostile), htmlFile, htmlFile.parentFile)
            val html = htmlFile.readText()
            html shouldNotContain "</script><img"
            html shouldNotContain "</script><b>alert"
            html shouldContain "&lt;/script&gt;"
        }
    })
