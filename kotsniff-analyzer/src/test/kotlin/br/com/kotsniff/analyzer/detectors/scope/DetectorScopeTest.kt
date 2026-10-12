package br.com.kotsniff.analyzer.detectors.scope

import br.com.kotsniff.analyzer.AnalysisSettings
import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies that diagnostics stay within the intended leaf-test scope, and that
 * assertion counting follows the single documented policy (each assertion call
 * / matcher is one unit; compound helpers such as `assertSoftly { }` count once).
 */
class DetectorScopeTest :
    FunSpec({
        val engine = KotSniffEngine.createDefault()

        fun count(
            code: String,
            rule: TestSmell,
            settings: AnalysisSettings = AnalysisSettings(),
        ): Int = KotSniffEngine.createDefault(settings).analyze(code, "Test.kt").count { it.ruleId == rule }

        test("a WordSpec container is not treated as a leaf (no Assertion Roulette leak)") {
            val code =
                """
                class W : WordSpec({
                    "container" should {
                        "a" { 1 shouldBe 1 }
                        "b" { 2 shouldBe 2 }
                        "c" { 3 shouldBe 3 }
                        "d" { 4 shouldBe 4 }
                    }
                })
                """.trimIndent()
            count(code, TestSmell.ASSERTION_ROULETTE) shouldBe 0
        }

        test("a WordSpec container does not compare assertions across sibling leaves") {
            val code =
                """
                class W : WordSpec({
                    "container" should {
                        "a" { value() shouldBe 1 }
                        "b" { value() shouldBe 1 }
                    }
                })
                """.trimIndent()
            count(code, TestSmell.DUPLICATE_ASSERT) shouldBe 0
        }

        test("two sibling tests with different assertions are analyzed independently") {
            val code =
                """
                class S : FunSpec({
                    test("a") { 1 shouldBe 1 }
                    test("b") { 2 shouldBe 2 }
                })
                """.trimIndent()
            count(code, TestSmell.DUPLICATE_ASSERT) shouldBe 0
            count(code, TestSmell.ASSERTION_ROULETTE) shouldBe 0
        }

        test("a nested container does not leak; the offending leaf is reported once") {
            val code =
                """
                class S : FunSpec({
                    context("c") {
                        test("many") {
                            1 shouldBe 1
                            2 shouldBe 2
                            3 shouldBe 3
                            4 shouldBe 4
                        }
                        test("few") { 1 shouldBe 1 }
                    }
                })
                """.trimIndent()
            count(code, TestSmell.ASSERTION_ROULETTE) shouldBe 1
        }

        test("assertSoftly counts as a single logical assertion") {
            val code =
                """
                class S : FunSpec({
                    test("soft") {
                        assertSoftly {
                            1 shouldBe 1
                            2 shouldBe 2
                            3 shouldBe 3
                            4 shouldBe 4
                        }
                    }
                })
                """.trimIndent()
            count(code, TestSmell.ASSERTION_ROULETTE) shouldBe 0
            count(code, TestSmell.UNKNOWN_TEST) shouldBe 0
        }

        test("nested assertions inside another assertion are not double counted") {
            val code =
                """
                class S : FunSpec({
                    test("nested") {
                        assert(1 shouldBe 1)
                        assert(2 shouldBe 2)
                        assert(3 shouldBe 3)
                    }
                })
                """.trimIndent()
            // Three logical assertions (double counting would yield six and trigger roulette).
            count(code, TestSmell.ASSERTION_ROULETTE) shouldBe 0
        }

        test("Assertion Roulette respects the exact threshold") {
            val base = { n: Int ->
                val body = (1..n).joinToString("\n") { "    $it shouldBe $it" }
                """
                class S : FunSpec({
                    test("t") {
                $body
                    }
                })
                """.trimIndent()
            }
            count(base(3), TestSmell.ASSERTION_ROULETTE) shouldBe 0
            count(base(4), TestSmell.ASSERTION_ROULETTE) shouldBe 1
        }

        test("delay(...) stays opt-in") {
            val code =
                """
                class S : FunSpec({
                    test("d") {
                        delay(10)
                        1 shouldBe 1
                    }
                })
                """.trimIndent()
            count(code, TestSmell.SLEEPY_TEST) shouldBe 0
            count(code, TestSmell.SLEEPY_TEST, AnalysisSettings(reportDelay = true)) shouldBe 1
        }

        test("an assertion hidden in a helper still reports Unknown Test (documented limitation)") {
            val code =
                """
                class S : FunSpec({
                    test("helper") {
                        val a = compute()
                        assertResult(a)
                    }
                })
                """.trimIndent()
            count(code, TestSmell.UNKNOWN_TEST) shouldBe 1
        }

        test("engine default is unchanged by the shared assertion policy (smoke)") {
            val code =
                """
                class S : StringSpec({
                    "no assertion" { val a = 1 }
                })
                """.trimIndent()
            engine.analyze(code, "Test.kt").map { it.ruleId } shouldBe listOf(TestSmell.UNKNOWN_TEST)
        }
    })
