package br.com.kotsniff.analyzer.detectors.boundaries

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Boundary checks for the individual detectors, tied to the documented policy. */
class DetectorBoundariesTest :
    FunSpec({
        val engine = KotSniffEngine.createDefault()

        fun count(
            code: String,
            rule: TestSmell,
        ): Int = engine.analyze(code, "Test.kt").count { it.ruleId == rule }

        test("an unrelated x-prefixed call inside a test is not an ignored test") {
            val code =
                """
                class S : FunSpec({
                    test("uses a helper") {
                        xray()
                        1 shouldBe 1
                    }
                })
                """.trimIndent()
            count(code, TestSmell.IGNORED_TEST) shouldBe 0
        }

        test("try/catch in a container block is not reported (leaf scope only)") {
            val code =
                """
                class S : BehaviorSpec({
                    given("setup with try") {
                        try { risky() } catch (e: Exception) { println(e) }
                        When("x") {
                            then("leaf") { 1 shouldBe 1 }
                        }
                    }
                })
                """.trimIndent()
            count(code, TestSmell.EXCEPTION_HANDLING) shouldBe 0
        }

        test("try/catch in a leaf test is reported") {
            val code =
                """
                class S : FunSpec({
                    test("leaf") {
                        try { risky() } catch (e: Exception) { println(e) }
                        1 shouldBe 1
                    }
                })
                """.trimIndent()
            count(code, TestSmell.EXCEPTION_HANDLING) shouldBe 1
        }

        test("a conditional used as a value is not reported; a statement one is") {
            val asValue =
                """
                class S : FunSpec({
                    test("t") {
                        val n = if (flag()) 1 else 2
                        n shouldBe 2
                    }
                })
                """.trimIndent()
            val asStatement =
                """
                class S : FunSpec({
                    test("t") {
                        if (flag()) { doSomething() }
                        1 shouldBe 1
                    }
                })
                """.trimIndent()
            count(asValue, TestSmell.CONDITIONAL_TEST_LOGIC) shouldBe 0
            count(asStatement, TestSmell.CONDITIONAL_TEST_LOGIC) shouldBe 1
        }

        test("negative numeric literal is inspected by magnitude; -1 stays allowed") {
            val minusFive =
                """
                class S : FunSpec({
                    test("t") { value shouldBe -5 }
                })
                """.trimIndent()
            val minusOne =
                """
                class S : FunSpec({
                    test("t") { value shouldBe -1 }
                })
                """.trimIndent()
            count(minusFive, TestSmell.MAGIC_NUMBER_TEST) shouldBe 1
            count(minusOne, TestSmell.MAGIC_NUMBER_TEST) shouldBe 0
        }
    })
