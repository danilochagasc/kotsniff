package br.com.kotsniff.analyzer.visitors

import br.com.kotsniff.analyzer.parser.KotlinParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Focused regression tests for how the extractor classifies blocks as
 * **containers** vs. **leaf tests** across Kotest styles.
 *
 * The WordSpec/FreeSpec infix forms (`"name" - { }`, `"name" should { }`,
 * `"name" When { }`) are always containers; their leaves are plain
 * string-invokes. Misclassifying a container as terminal would let nested
 * tests' assertions leak into the parent (see [DetectorScopeTest]).
 */
class KotestBlockExtractorClassificationTest :
    FunSpec({
        fun blocks(code: String): List<KotestBlock> {
            val file = KotlinParser.parse(code)
            val visitor = KotestBlockExtractor()
            file.accept(visitor)
            return visitor.foundTests
        }

        fun classify(code: String): List<Pair<String, Boolean>> = blocks(code).map { it.name to it.isTerminal }

        test("WordSpec infix should/When are containers; string-invokes are leaves") {
            val code =
                """
                class W : WordSpec({
                    "subject" should {
                        "leaf one" { 1 shouldBe 1 }
                        "leaf two" { 2 shouldBe 2 }
                    }
                    "other" When {
                        "nested" should {
                            "deep leaf" { 3 shouldBe 3 }
                        }
                    }
                })
                """.trimIndent()

            classify(code) shouldBe
                listOf(
                    "subject" to false,
                    "leaf one" to true,
                    "leaf two" to true,
                    "other" to false,
                    "nested" to false,
                    "deep leaf" to true,
                )
        }

        test("FreeSpec container is a container and its leaf is terminal") {
            val code =
                """
                class F : FreeSpec({
                    "container" - {
                        "leaf" { 1 shouldBe 1 }
                    }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("container" to false, "leaf" to true)
        }

        test("FunSpec test is terminal, context is a container") {
            val code =
                """
                class S : FunSpec({
                    test("a") { 1 shouldBe 1 }
                    context("c") { test("b") { 2 shouldBe 2 } }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("a" to true, "c" to false, "b" to true)
        }

        test("StringSpec string-invokes are terminals") {
            val code =
                """
                class S : StringSpec({
                    "a" { 1 shouldBe 1 }
                    "b" { 2 shouldBe 2 }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("a" to true, "b" to true)
        }

        test("DescribeSpec describe/context are containers, it is terminal") {
            val code =
                """
                class S : DescribeSpec({
                    describe("d") {
                        context("c") {
                            it("t") { 1 shouldBe 1 }
                        }
                    }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("d" to false, "c" to false, "t" to true)
        }

        test("BehaviorSpec given/When are containers, then is terminal") {
            val code =
                """
                class S : BehaviorSpec({
                    given("g") {
                        When("w") {
                            then("t") { 1 shouldBe 1 }
                        }
                    }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("g" to false, "w" to false, "t" to true)
        }

        test("ShouldSpec should() is terminal, context is a container") {
            val code =
                """
                class S : ShouldSpec({
                    should("a") { 1 shouldBe 1 }
                    context("c") { should("b") { 2 shouldBe 2 } }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("a" to true, "c" to false, "b" to true)
        }

        test("ExpectSpec context is a container, expect is terminal") {
            val code =
                """
                class S : ExpectSpec({
                    context("c") { expect("e") { 1 shouldBe 1 } }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("c" to false, "e" to true)
        }

        test("FeatureSpec feature is a container, scenario is terminal") {
            val code =
                """
                class S : FeatureSpec({
                    feature("f") { scenario("s") { 1 shouldBe 1 } }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("f" to false, "s" to true)
        }

        test("AnnotationSpec @Test function is terminal") {
            val code =
                """
                class S : AnnotationSpec() {
                    @Test
                    fun leaf() { 1 shouldBe 1 }
                }
                """.trimIndent()

            classify(code) shouldBe listOf("leaf" to true)
        }

        test("a non-spec class with DSL-like method names yields no blocks") {
            val code =
                """
                class Helper {
                    fun test() {}
                    fun should() {}
                    fun it() {}
                    fun context() {}
                }
                """.trimIndent()

            blocks(code).size shouldBe 0
        }

        test("local spec inheritance is resolved transitively") {
            val code =
                """
                class Base : FunSpec()
                class Middle : Base()
                class Leaf : Middle({
                    test("works") { 1 shouldBe 1 }
                })
                """.trimIndent()

            classify(code) shouldBe listOf("works" to true)
        }
    })
