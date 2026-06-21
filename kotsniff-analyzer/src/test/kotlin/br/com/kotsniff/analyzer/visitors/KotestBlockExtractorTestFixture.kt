package br.com.kotsniff.analyzer.visitors

/**
 * Sample Kotest source code snippets used as fixtures for
 * [KotestBlockExtractorTest].
 */
object KotestBlockExtractorTestFixture {
    fun stringSpec() =
        """
        class SampleStringSpec : StringSpec({
            "should add numbers correctly" {
                val result = 1 + 1
            }
            "should subtract numbers correctly" {
                val result = 2 - 1
            }
        })
        """.trimIndent()

    fun behaviorSpec() =
        """
        class SampleBehaviorSpec : BehaviorSpec({
            given("a calculator") {
                `when`("adding 1 and 2") {
                    then("should return 3") {
                        val result = 1 + 2
                    }
                }
                `when`("subtracting 1 from 2") {
                    then("should return 1") {
                        val result = 2 - 1
                    }
                }
            }
        })
        """.trimIndent()

    fun funSpec() =
        """
        class SampleFunSpec : FunSpec({
            test("addition works") {
                val result = 1 + 1
            }
            context("string operations") {
                test("concatenation works") {
                    val result = "a" + "b"
                }
            }
        })
        """.trimIndent()

    fun notASpec() =
        """
        class NotASpec {
            fun test(name: String) {
                println(name)
            }
        }
        """.trimIndent()

    fun initPatternDescribeSpec() =
        """
        class SampleInitDescribeSpec : DescribeSpec() {
            init {
                describe("a feature") {
                    it("should work") {
                        val result = 1 + 1
                    }
                }
            }
        }
        """.trimIndent()

    fun initPatternBehaviorSpec() =
        """
        class SampleInitBehaviorSpec : BehaviorSpec() {
            init {
                given("a scenario") {
                    `when`("something happens") {
                        then("it should work") {
                            val result = 1 + 1
                        }
                    }
                }
            }
        }
        """.trimIndent()

    fun initPatternFunSpec() =
        """
        class SampleInitFunSpec : FunSpec() {
            init {
                test("works inside init") {
                    val result = 1 + 1
                }
            }
        }
        """.trimIndent()

    fun featureSpec() =
        """
        class SampleFeatureSpec : FeatureSpec({
            feature("a calculator") {
                scenario("addition works") {
                    val result = 1 + 1
                }
            }
        })
        """.trimIndent()

    fun expectSpec() =
        """
        class SampleExpectSpec : ExpectSpec({
            context("a calculator") {
                expect("addition works") {
                    val result = 1 + 1
                }
            }
        })
        """.trimIndent()
}
