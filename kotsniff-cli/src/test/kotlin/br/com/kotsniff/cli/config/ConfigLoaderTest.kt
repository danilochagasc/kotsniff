package br.com.kotsniff.cli.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class ConfigLoaderTest :
    FunSpec({
        test("returns null when the file does not exist") {
            ConfigLoader.load(File("does-not-exist-${System.nanoTime()}.json")) shouldBe null
        }

        test("parses a valid configuration") {
            val file =
                writeTemp(
                    """
                    { "exclude": ["**/build/**"], "disabledRules": ["MAGIC_NUMBER_TEST"], "maxAssertions": 5, "reportDelay": true, "allowedNumbers": ["404", "200"], "reportMaxLines": 500 }
                    """.trimIndent(),
                )
            val config = ConfigLoader.load(file)!!
            config.exclude shouldBe listOf("**/build/**")
            config.disabledRules shouldBe listOf("MAGIC_NUMBER_TEST")
            config.maxAssertions shouldBe 5
            config.reportDelay shouldBe true
            config.allowedNumbers shouldBe listOf("404", "200")
            config.reportMaxLines shouldBe 500
        }

        test("parses severity overrides") {
            val file =
                writeTemp(
                    """
                    { "severities": { "MAGIC_NUMBER_TEST": "high", "Unknown Test": "info" } }
                    """.trimIndent(),
                )
            ConfigLoader.load(file)!!.severities shouldBe
                mapOf("MAGIC_NUMBER_TEST" to "high", "Unknown Test" to "info")
        }

        test("save writes a config that reloads identically") {
            val file = File.createTempFile("kotsniff-save", ".json").apply { deleteOnExit() }
            val config =
                FileConfig(
                    exclude = listOf("**/build/**"),
                    maxAssertions = 5,
                    severities = mapOf("UNKNOWN_TEST" to "low"),
                )
            ConfigLoader.save(file, config)

            val reloaded = ConfigLoader.load(file)!!
            reloaded.exclude shouldBe listOf("**/build/**")
            reloaded.maxAssertions shouldBe 5
            reloaded.severities shouldBe mapOf("UNKNOWN_TEST" to "low")
        }

        test("save omits fields left at their defaults") {
            val file = File.createTempFile("kotsniff-save", ".json").apply { deleteOnExit() }
            ConfigLoader.save(file, FileConfig(severities = mapOf("UNKNOWN_TEST" to "low")))

            val text = file.readText()
            text.contains("\"disabledRules\"") shouldBe false
            text.contains("\"maxAssertions\"") shouldBe false
            text.contains("\"severities\"") shouldBe true
        }

        test("throws a meaningful error on malformed configuration") {
            val file = writeTemp("{ not json ")
            shouldThrow<IllegalArgumentException> { ConfigLoader.load(file) }
        }

        test("tolerates a UTF-8 BOM") {
            val file = File.createTempFile("kotsniff-bom", ".json").apply { deleteOnExit() }
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            file.writeBytes(bom + """{ "maxAssertions": 7 }""".toByteArray(Charsets.UTF_8))

            ConfigLoader.load(file)!!.maxAssertions shouldBe 7
        }
    })

private fun writeTemp(content: String): File {
    val file = File.createTempFile("kotsniff-config", ".json")
    file.deleteOnExit()
    file.writeText(content)
    return file
}
