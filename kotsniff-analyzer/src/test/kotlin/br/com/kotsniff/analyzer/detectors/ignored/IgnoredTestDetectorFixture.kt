package br.com.kotsniff.analyzer.detectors.ignored

object IgnoredTestDetectorFixture {
    const val FILE_PATH_STRING_SPEC = "SampleIgnoredStringSpec.kt"
    const val FILE_PATH_FUN_SPEC = "SampleIgnoredFunSpec.kt"
    const val FILE_PATH_BEHAVIOR_SPEC = "SampleIgnoredBehaviorSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "SampleIgnoredDescribeSpec.kt"

    fun ignoredStringSpec() =
        """
        class SampleIgnoredStringSpec : StringSpec({
            !"ignored string test" {
                val result = 1 + 1
            }
        })
        """.trimIndent()

    fun ignoredFunSpec() =
        """
        class SampleIgnoredFunSpec : FunSpec({
            xtest("ignored fun test") {
                val result = 1 + 1
            }
        })
        """.trimIndent()

    fun ignoredBehaviorSpec() =
        """
        class SampleIgnoredBehaviorSpec : BehaviorSpec({
            xgiven("an ignored test") {
                When("it is ignored") {
                    then("it should be detected") {
                        val result = 1 + 1
                    }
                }
            }
            given("a normal given") {
                xwhen("an ignored when") {
                    then("it should be detected") {
                        val result = 1 + 1
                    }
                }
            }
            given("another normal given") {
                When("a normal when") {
                    xthen("an ignored then") {
                        val result = 1 + 1
                    }
                }
            }
        })
        """.trimIndent()

    fun ignoredDescribeSpec() =
        """
        class SampleIgnoredDescribeSpec : DescribeSpec({
            xdescribe("an ignored describe") {
                it("should be detected") {
                    val result = 1 + 1
                }
            }
            describe("a normal describe") {
                xit("an ignored it") {
                    val result = 1 + 1
                }
            }
        })
        """.trimIndent()

    const val FILE_PATH_CLASS_IGNORED = "SampleClassIgnoredSpec.kt"

    fun classIgnoredSpec() =
        """
        import io.kotest.core.annotation.Ignored
        
        @Ignored
        class SampleClassIgnoredSpec : FunSpec({
            test("test 1") {
                val result = 1 + 1
            }
            test("test 2") {
                val result = 2 + 2
            }
        })
        """.trimIndent()
}
