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
}
