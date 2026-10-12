package br.com.kotsniff.analyzer

import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Documents and verifies how MockK constructs are interpreted.
 *
 * The requirement is that **ordinary MockK usage is understood** — verifications
 * behave like assertions and stubbing is not penalised — rather than inventing a
 * new smell for MockK. See IMPLEMENTATION_REPORT.md for the rationale.
 */
class MockkRecognitionTest :
    FunSpec({
        val engine = KotSniffEngine.createDefault()

        fun countByRule(
            code: String,
            rule: TestSmell,
        ): Int = engine.analyze(code, "MockSpec.kt").count { it.ruleId == rule }

        fun spec(body: String): String =
            """
            import io.kotest.core.spec.style.StringSpec

            class MockSpec : StringSpec({
                "a test" {
                    $body
                }
            })
            """.trimIndent()

        test("coVerify counts as an assertion (no Unknown Test)") {
            countByRule(spec("coVerify { repository.save(any()) }"), TestSmell.UNKNOWN_TEST) shouldBe 0
        }

        test("verifyOrder, verifySequence and confirmVerified are recognized") {
            val body =
                """
                verifyOrder { repository.save(any()) }
                verifySequence { repository.save(any()) }
                confirmVerified(repository)
                """.trimIndent()
            countByRule(spec(body), TestSmell.UNKNOWN_TEST) shouldBe 0
        }

        test("stubbing alone is not an assertion -> Unknown Test") {
            countByRule(spec("every { repository.save(any()) } returns Unit"), TestSmell.UNKNOWN_TEST) shouldBe 1
        }

        test("a domain method named like a verification is not a verification -> Unknown Test") {
            countByRule(spec("val ok = verifyInput(3)\ndoSomething(ok)"), TestSmell.UNKNOWN_TEST) shouldBe 1
        }

        test("stubbing does not inflate the assertion count for Assertion Roulette") {
            val body =
                """
                every { a() } returns 1
                every { b() } returns 2
                every { c() } returns 3
                every { d() } returns 4
                verify { a() }
                """.trimIndent()
            countByRule(spec(body), TestSmell.ASSERTION_ROULETTE) shouldBe 0
        }

        test("mock creation alone is not an assertion -> Unknown Test") {
            countByRule(spec("val repo = mockk<Repository>()\nrepo.delete()"), TestSmell.UNKNOWN_TEST) shouldBe 1
        }
    })
