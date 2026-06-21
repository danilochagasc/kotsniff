package br.com.kotsniff.analyzer.detectors.magicnumber

object MagicNumberTestDetectorFixture {
    const val FILE_PATH_FUN_SPEC = "src/test/kotlin/SampleFunSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "src/test/kotlin/SampleDescribeSpec.kt"
    const val FILE_PATH_FREE_SPEC = "src/test/kotlin/SampleFreeSpec.kt"
    const val FILE_PATH_INIT_SPEC = "src/test/kotlin/SampleInitSpec.kt"

    fun magicNumberFunSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.magicnumber

        import io.kotest.core.spec.style.FunSpec
        import io.kotest.matchers.shouldBe

        class SampleFunSpec : FunSpec({
            test("should have magic number") {
                val responseCode = 404
                responseCode shouldBe 404 // magic number
                
                val count = 0
                count shouldBe 0 // allowed
                
                val other = 1
                other shouldBe 1 // allowed
            }
        })
        """.trimIndent()

    fun magicNumberDescribeSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.magicnumber

        import io.kotest.core.spec.style.DescribeSpec
        import io.kotest.matchers.shouldBe

        class SampleDescribeSpec : DescribeSpec({
            describe("a context") {
                it("has magic number assert") {
                    val price = 10.5
                    assert(price == 10.5) // magic number
                }
            }
        })
        """.trimIndent()

    fun magicNumberFreeSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.magicnumber

        import io.kotest.core.spec.style.FreeSpec
        import io.kotest.matchers.shouldBe

        class SampleFreeSpec : FreeSpec({
            "a context" - {
                "has magic number should assert" {
                    val timeout = 5000L
                    timeout.shouldBe(5000L) // magic number
                    
                    val hundred = 100
                    hundred.shouldBe(100) // allowed
                }
            }
        })
        """.trimIndent()

    fun magicNumberInitSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.magicnumber

        import io.kotest.core.spec.style.FunSpec
        import io.kotest.matchers.shouldBe

        class SampleInitSpec : FunSpec() {
            init {
                test("has magic number expect") {
                    expect(42 == 42) { "magic" } // 42 is magic number (twice)
                }
            }
        }
        """.trimIndent()
}
