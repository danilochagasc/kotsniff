package br.com.kotsniff.analyzer.detectors.conditional

object ConditionalTestLogicDetectorFixture {
    const val FILE_PATH_STRING_SPEC = "SampleConditionalStringSpec.kt"
    const val FILE_PATH_FUN_SPEC = "SampleConditionalFunSpec.kt"
    const val FILE_PATH_BEHAVIOR_SPEC = "SampleConditionalBehaviorSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "SampleConditionalDescribeSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC_INIT = "SampleConditionalDescribeSpecInit.kt"

    fun conditionalStringSpec() =
        """
        class SampleConditionalStringSpec : StringSpec({
            "should detect if" {
                if (1 == 1) {
                    println("yes")
                }
            }
        })
        """.trimIndent()

    fun conditionalFunSpec() =
        """
        class SampleConditionalFunSpec : FunSpec({
            test("should detect when") {
                when (1) {
                    1 -> println("yes")
                }
            }
        })
        """.trimIndent()

    fun conditionalBehaviorSpec() =
        """
        class SampleConditionalBehaviorSpec : BehaviorSpec({
            given("a conditional test") {
                When("it has an if") {
                    then("it should be detected") {
                        if (true) println("true") else println("false")
                    }
                }
            }
        })
        """.trimIndent()

    fun conditionalDescribeSpec() =
        """
        class SampleConditionalDescribeSpec : DescribeSpec({
            describe("a conditional test") {
                it("should be detected") {
                    when (true) {
                        true -> println("true")
                    }
                }
            }
        })
        """.trimIndent()

    fun conditionalDescribeSpecInit() =
        """
        class SampleConditionalDescribeSpecInit : DescribeSpec() {
            init {
                describe("a conditional test") {
                    it("should be detected") {
                        if (true) {
                            println("yes")
                        }
                    }
                }
            }
        }
        """.trimIndent()
}
