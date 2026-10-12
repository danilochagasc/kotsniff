package br.com.kotsniff.analyzer

import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DetectorRegistryTest :
    FunSpec({
        val code =
            """
            import io.kotest.core.spec.style.FunSpec

            class SampleSpec : FunSpec({
                test("no assertion") {
                    val a = 1
                }
            })
            """.trimIndent()

        test("a disabled rule produces no diagnostics of that type") {
            val engine = KotSniffEngine.createDefault(AnalysisSettings(disabledRules = setOf(TestSmell.UNKNOWN_TEST)))
            val results = engine.analyze(code, "SampleSpec.kt")
            results.none { it.ruleId == TestSmell.UNKNOWN_TEST } shouldBe true
        }

        test("an enabled rule detects the smell") {
            val engine = KotSniffEngine.createDefault()
            val results = engine.analyze(code, "SampleSpec.kt")
            results.count { it.ruleId == TestSmell.UNKNOWN_TEST } shouldBe 1
        }

        test("all registered detectors expose an id") {
            val ids = DetectorRegistry.create(AnalysisSettings()).all().map { it.id }
            ids.distinct().size shouldBe ids.size
        }

        test("a custom allowedNumbers replaces the magic-number allowlist") {
            val codeWith404 =
                """
                class S : io.kotest.core.spec.style.StringSpec({
                    "a test" {
                        val code = 404
                        code shouldBe 404
                    }
                })
                """.trimIndent()

            val withDefault = KotSniffEngine.createDefault()
            withDefault.analyze(codeWith404, "S.kt").count { it.ruleId == TestSmell.MAGIC_NUMBER_TEST } shouldBe 1

            val allowing404 = KotSniffEngine.createDefault(AnalysisSettings(allowedNumbers = setOf("404")))
            allowing404.analyze(codeWith404, "S.kt").count { it.ruleId == TestSmell.MAGIC_NUMBER_TEST } shouldBe 0
        }
    })
