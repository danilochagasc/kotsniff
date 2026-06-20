package br.com.kotsniff.analyzer.detectors.sleepy

object SleepyTestDetectorFixture {
    const val FILE_PATH_STRING_SPEC = "SampleSleepyStringSpec.kt"
    const val FILE_PATH_FUN_SPEC = "SampleSleepyFunSpec.kt"
    const val FILE_PATH_BEHAVIOR_SPEC = "SampleSleepyBehaviorSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "SampleSleepyDescribeSpec.kt"

    const val FILE_PATH_DESCRIBE_SPEC_INIT = "SampleSleepyDescribeSpecInit.kt"

    fun sleepyStringSpec() =
        """
        class SampleSleepyStringSpec : StringSpec({
            "should wait a bit" {
                Thread.sleep(1000)
            }
        })
        """.trimIndent()

    fun sleepyFunSpec() =
        """
        class SampleSleepyFunSpec : FunSpec({
            test("should wait a bit") {
                delay(500L)
            }
        })
        """.trimIndent()

    fun sleepyBehaviorSpec() =
        """
        class SampleSleepyBehaviorSpec : BehaviorSpec({
            given("a sleepy test") {
                When("it sleeps") {
                    then("it should be detected") {
                        Thread.sleep(500)
                    }
                }
            }
        })
        """.trimIndent()

    fun sleepyDescribeSpec() =
        """
        class SampleSleepyDescribeSpec : DescribeSpec({
            describe("a sleepy test") {
                it("should be detected") {
                    delay(100L)
                }
            }
        })
        """.trimIndent()

    fun sleepyDescribeSpecInit() =
        """
        class SampleSleepyDescribeSpecInit : DescribeSpec() {
            init {
                describe("a sleepy test") {
                    it("should be detected") {
                        Thread.sleep(100)
                    }
                }
            }
        }
        """.trimIndent()
}
