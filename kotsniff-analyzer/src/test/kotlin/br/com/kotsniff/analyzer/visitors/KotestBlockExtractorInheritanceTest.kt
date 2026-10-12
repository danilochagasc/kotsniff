package br.com.kotsniff.analyzer.visitors

import br.com.kotsniff.analyzer.parser.KotlinParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class KotestBlockExtractorInheritanceTest :
    FunSpec({
        test("detects tests inside a class extending a local base spec") {
            val code =
                """
                import io.kotest.core.spec.style.FunSpec
                import io.kotest.matchers.shouldBe

                class BaseTest : FunSpec()

                class RealSpec : BaseTest({
                    test("works") {
                        1 shouldBe 1
                    }
                })
                """.trimIndent()

            val ktFile = KotlinParser.parse(code)
            val visitor = KotestBlockExtractor()
            ktFile.accept(visitor)

            visitor.foundTests.map { it.name } shouldContainExactly listOf("works")
        }

        test("does not extract tests from classes that are not specs") {
            val code =
                """
                class Helper {
                    fun test() {
                        println("not a kotest test")
                    }
                }
                """.trimIndent()

            val ktFile = KotlinParser.parse(code)
            val visitor = KotestBlockExtractor()
            ktFile.accept(visitor)

            visitor.foundTests.size shouldBe 0
        }
    })
