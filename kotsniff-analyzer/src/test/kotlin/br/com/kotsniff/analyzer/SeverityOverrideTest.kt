package br.com.kotsniff.analyzer

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SeverityOverrideTest :
    FunSpec({
        val code =
            """
            import io.kotest.core.spec.style.StringSpec

            class SampleSpec : StringSpec({
                "no assertion" {
                    val a = 1
                }
            })
            """.trimIndent()

        test("uses the default severity when nothing is overridden") {
            val diagnostic =
                KotSniffEngine
                    .createDefault()
                    .analyze(code, "SampleSpec.kt")
                    .first { it.ruleId == TestSmell.UNKNOWN_TEST }
            diagnostic.severity shouldBe TestSmell.UNKNOWN_TEST.defaultSeverity
        }

        test("applies a per-rule severity override") {
            val engine =
                KotSniffEngine.createDefault(
                    AnalysisSettings(severityOverrides = mapOf(TestSmell.UNKNOWN_TEST to Severity.LOW)),
                )
            val diagnostic =
                engine.analyze(code, "SampleSpec.kt").first { it.ruleId == TestSmell.UNKNOWN_TEST }
            diagnostic.severity shouldBe Severity.LOW
        }

        test("rules not listed in the override map keep their default") {
            val engine =
                KotSniffEngine.createDefault(
                    AnalysisSettings(severityOverrides = mapOf(TestSmell.MAGIC_NUMBER_TEST to Severity.INFO)),
                )
            val diagnostic =
                engine.analyze(code, "SampleSpec.kt").first { it.ruleId == TestSmell.UNKNOWN_TEST }
            diagnostic.severity shouldBe TestSmell.UNKNOWN_TEST.defaultSeverity
        }
    })
