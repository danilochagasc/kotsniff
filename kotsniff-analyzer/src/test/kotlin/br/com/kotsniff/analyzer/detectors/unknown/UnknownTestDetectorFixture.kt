package br.com.kotsniff.analyzer.detectors.unknown

object UnknownTestDetectorFixture {
    const val FILE_PATH_STRING_SPEC = "SampleUnknownTestStringSpec.kt"
    const val FILE_PATH_FUN_SPEC = "SampleUnknownTestFunSpec.kt"
    const val FILE_PATH_BEHAVIOR_SPEC = "SampleUnknownTestBehaviorSpec.kt"
    const val FILE_PATH_DESCRIBE_SPEC = "SampleUnknownTestDescribeSpec.kt"

    fun unknownTestStringSpec() =
        """
        class SampleUnknownTestStringSpec : StringSpec({
            "a test with no assertions" {
                val a = 1
                val b = 2
                val c = a + b
            }
        })
        """.trimIndent()

    fun unknownTestFunSpec() =
        """
        class SampleUnknownTestFunSpec : FunSpec({
            test("a test with no assertions") {
                val a = 1
                val b = 2
                val c = a + b
            }
        })
        """.trimIndent()

    fun unknownTestBehaviorSpec() =
        """
        class SampleUnknownTestBehaviorSpec : BehaviorSpec({
            given("a scenario") {
                when("something happens") {
                    then("a test with no assertions") {
                        val a = 1
                        val b = 2
                        val c = a + b
                    }
                }
            }
        })
        """.trimIndent()

    fun unknownTestDescribeSpec() =
        """
        class SampleUnknownTestDescribeSpec : DescribeSpec({
            describe("a scenario") {
                it("a test with no assertions") {
                    val a = 1
                    val b = 2
                    val c = a + b
                }
            }
        })
        """.trimIndent()

    fun validTestWithMockkVerification() =
        """
        package br.com.kotsniff.analyzer.detectors.unknown

        import io.kotest.core.spec.style.StringSpec
        import io.mockk.coEvery
        import io.mockk.coVerify
        import io.mockk.mockk

        class SampleMockkSpec : StringSpec({
            "saving a user should trigger repository" {
                val repository = mockk<UserRepository>()
                val service = UserService(repository)
                
                coEvery { repository.save(any()) } returns Unit
                
                service.saveUser("Danilo")
                
                coVerify { repository.save(any()) }
            }
        })
        """.trimIndent()
}
