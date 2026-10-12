package br.com.kotsniff.analyzer.detectors.exception

object ExceptionHandlingDetectorFixture {
    const val FILE_PATH_STRING_SPEC = "SampleExceptionHandlingStringSpec.kt"
    const val FILE_PATH_FUN_SPEC = "SampleExceptionHandlingFunSpec.kt"
    const val FILE_PATH_BEHAVIOR_SPEC = "SampleExceptionHandlingBehaviorSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "SampleExceptionHandlingDescribeSpec.kt"

    fun exceptionHandlingStringSpec() =
        """
        class SampleExceptionHandlingStringSpec : StringSpec({
            "a test with try catch" {
                try {
                    val a = 1 / 0
                } catch (e: Exception) {
                    e.message shouldBe "/ by zero"
                }
            }
        })
        """.trimIndent()

    fun exceptionHandlingFunSpec() =
        """
        class SampleExceptionHandlingFunSpec : FunSpec({
            test("a test with try catch") {
                try {
                    val a = 1 / 0
                } catch (e: Exception) {
                    e.message shouldBe "/ by zero"
                }
            }
        })
        """.trimIndent()

    fun exceptionHandlingBehaviorSpec() =
        """
        class SampleExceptionHandlingBehaviorSpec : BehaviorSpec({
            given("a scenario") {
                `when`("something happens") {
                    then("a test with try catch") {
                        try {
                            val a = 1 / 0
                        } catch (e: Exception) {
                            e.message shouldBe "/ by zero"
                        }
                    }
                }
            }
        })
        """.trimIndent()

    fun exceptionHandlingDescribeSpec() =
        """
        class SampleExceptionHandlingDescribeSpec : DescribeSpec({
            describe("a scenario") {
                it("a test with try catch") {
                    try {
                        val a = 1 / 0
                    } catch (e: Exception) {
                        e.message shouldBe "/ by zero"
                    }
                }
            }
        })
        """.trimIndent()
}
