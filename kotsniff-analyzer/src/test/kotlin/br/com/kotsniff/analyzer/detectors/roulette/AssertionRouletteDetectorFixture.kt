package br.com.kotsniff.analyzer.detectors.roulette

object AssertionRouletteDetectorFixture {
    const val FILE_PATH_ROULETTE_DESCRIBE_SPEC = "src/test/kotlin/RouletteDescribeSpecTest.kt"
    const val FILE_PATH_ROULETTE_STRING_SPEC = "src/test/kotlin/RouletteStringSpecTest.kt"
    const val FILE_PATH_ROULETTE_INIT_SPEC = "src/test/kotlin/RouletteInitSpecTest.kt"

    fun rouletteDescribeSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.roulette

        import io.kotest.core.spec.style.DescribeSpec
        import io.kotest.matchers.shouldBe

        class RouletteDescribeSpecTest : DescribeSpec({
            describe("a test block") {
                it("has 4 assertions and fails default") {
                    val list = listOf(1, 2, 3)
                    list.size.shouldBe(3)
                    list[0].shouldBe(1)
                    list[1].shouldBe(2)
                    list[2].shouldBe(3)
                }
                it("has 2 assertions and passes default") {
                    val list = listOf(1, 2)
                    list.size.shouldBe(2)
                    list[0].shouldBe(1)
                }
            }
        })
        """.trimIndent()

    fun rouletteStringSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.roulette

        import io.kotest.core.spec.style.StringSpec
        import io.kotest.matchers.shouldBe

        class RouletteStringSpecTest : StringSpec({
            "has 4 assertions and fails default" {
                val name = "kotsniff"
                name.length.shouldBe(8)
                name.startsWith("kot").shouldBe(true)
                name.endsWith("sniff").shouldBe(true)
                name.shouldBe("kotsniff")
            }
        })
        """.trimIndent()

    fun rouletteInitSpec(): String =
        """
        package br.com.kotsniff.analyzer.detectors.roulette

        import io.kotest.core.spec.style.FunSpec
        import io.kotest.matchers.shouldBe

        class RouletteInitSpecTest : FunSpec() {
            init {
                test("has 4 assertions and fails default") {
                    val version = "1.0.0"
                    version.shouldBe("1.0.0")
                    version.length.shouldBe(5)
                    version.startsWith("1").shouldBe(true)
                    version.endsWith("0").shouldBe(true)
                }
            }
        }
        """.trimIndent()
}
