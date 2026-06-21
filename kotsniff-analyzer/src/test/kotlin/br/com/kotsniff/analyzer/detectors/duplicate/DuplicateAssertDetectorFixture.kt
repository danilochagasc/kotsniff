package br.com.kotsniff.analyzer.detectors.duplicate

object DuplicateAssertDetectorFixture {
    const val FILE_PATH_FUN_SPEC = "src/test/kotlin/SampleFunSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "src/test/kotlin/SampleDescribeSpec.kt"
    const val FILE_PATH_FREE_SPEC = "src/test/kotlin/SampleFreeSpec.kt"
    const val FILE_PATH_INIT_SPEC = "src/test/kotlin/SampleInitSpec.kt"

    fun duplicateAssertFunSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.duplicate

        import io.kotest.core.spec.style.FunSpec
        import io.kotest.matchers.shouldBe

        class SampleFunSpec : FunSpec({
            test("should have duplicate assert") {
                val a = 1
                a shouldBe 1
                val b = 2
                a shouldBe 1 // duplicate
            }
            
            test("should not have duplicate assert") {
                val a = 1
                val b = 2
                a shouldBe 1
                b shouldBe 2
            }
        })
        """.trimIndent()

    fun duplicateAssertDescribeSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.duplicate

        import io.kotest.core.spec.style.DescribeSpec
        import io.kotest.matchers.shouldBe

        class SampleDescribeSpec : DescribeSpec({
            describe("a context") {
                it("has duplicate call expression assert") {
                    val list = listOf(1, 2)
                    assert(list.size == 2)
                    assert(list.size == 2) // duplicate
                }
            }
        })
        """.trimIndent()

    fun duplicateAssertFreeSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.duplicate

        import io.kotest.core.spec.style.FreeSpec
        import io.kotest.matchers.shouldBe

        class SampleFreeSpec : FreeSpec({
            "a context" - {
                "has duplicate should assert" {
                    val name = "test"
                    name.shouldBe("test")
                    name.shouldBe("test") // duplicate
                }
            }
        })
        """.trimIndent()

    fun duplicateAssertInitSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.duplicate

        import io.kotest.core.spec.style.FunSpec
        import io.kotest.matchers.shouldBe

        class SampleInitSpec : FunSpec() {
            init {
                test("has duplicate expect") {
                    expect(1 == 1) { "should be true" }
                    expect(1 == 1) { "should be true" } // duplicate
                }
            }
        }
        """.trimIndent()
}
