package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.TestReports
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class SarifReportGeneratorTest :
    FunSpec({
        test("produces valid SARIF 2.1.0 with one result per diagnostic") {
            val report = TestReports.single(TestReports.diagnostic())
            val output = File.createTempFile("kotsniff-sarif", ".sarif").apply { deleteOnExit() }
            SarifReportGenerator().generate(report, output, output.parentFile)

            val root = Json.parseToJsonElement(output.readText()).jsonObject
            root["version"]?.jsonPrimitive?.content shouldBe "2.1.0"

            val run = root["runs"]!!.jsonArray[0].jsonObject
            val driver = run["tool"]!!.jsonObject["driver"]!!.jsonObject
            driver["name"]?.jsonPrimitive?.content shouldBe "KotSniff"

            val results = run["results"]!!.jsonArray
            results.size shouldBe 1
            results[0].jsonObject["ruleId"]?.jsonPrimitive?.content shouldBe "CONDITIONAL_TEST_LOGIC"
        }
    })
