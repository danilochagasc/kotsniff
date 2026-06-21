package br.com.kotsniff.analyzer.visitors

import br.com.kotsniff.analyzer.parser.KotlinParser
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractorTestFixture.expectSpec
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractorTestFixture.featureSpec
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractorTestFixture.initPatternBehaviorSpec
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractorTestFixture.initPatternDescribeSpec
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractorTestFixture.initPatternFunSpec
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractorTestFixture.stringSpec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class KotestBlockExtractorTest :
    DescribeSpec({
        describe("KotestBlockExtractor") {
            context("when parsing different Spec styles") {
                it("should find test blocks in a StringSpec") {
                    val ktFile = KotlinParser.parse(stringSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    visitor.foundTests shouldHaveSize 2
                    visitor.foundTests[0].name shouldBe "should add numbers correctly"
                    visitor.foundTests[1].name shouldBe "should subtract numbers correctly"
                }

                it("should find FunSpec tests") {
                    val code =
                        """
                        class SampleFunSpec : FunSpec({
                            test("this is a fun spec test") {
                                val x = 1
                            }
                            context("this is a context") {
                                test("nested test") {
                                    val y = 2
                                }
                            }
                        })
                        """.trimIndent()

                    val ktFile = KotlinParser.parse(code)
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 3
                    tests[0].name shouldBe "this is a fun spec test"
                    tests[1].name shouldBe "this is a context"
                    tests[2].name shouldBe "nested test"
                }

                it("should find BehaviorSpec tests") {
                    val code =
                        """
                        class SampleBehaviorSpec : BehaviorSpec({
                            given("a given") {
                                When("a when") {
                                    then("a then") {
                                        val x = 1
                                    }
                                }
                            }
                        })
                        """.trimIndent()

                    val ktFile = KotlinParser.parse(code)
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 3
                    tests[0].name shouldBe "a given"
                    tests[1].name shouldBe "a when"
                    tests[2].name shouldBe "a then"
                }

                it("should find DescribeSpec tests defined inside an init block") {
                    val ktFile = KotlinParser.parse(initPatternDescribeSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 2
                    tests[0].name shouldBe "a feature"
                    tests[1].name shouldBe "should work"
                }

                it("should find BehaviorSpec tests defined inside an init block") {
                    val ktFile = KotlinParser.parse(initPatternBehaviorSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 3
                    tests[0].name shouldBe "a scenario"
                    tests[1].name shouldBe "something happens"
                    tests[2].name shouldBe "it should work"
                }

                it("should find FunSpec tests defined inside an init block") {
                    val ktFile = KotlinParser.parse(initPatternFunSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 1
                    tests[0].name shouldBe "works inside init"
                }

                it("should find FeatureSpec tests") {
                    val ktFile = KotlinParser.parse(featureSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 2
                    tests[0].name shouldBe "a calculator"
                    tests[1].name shouldBe "addition works"
                }

                it("should find ExpectSpec tests") {
                    val ktFile = KotlinParser.parse(expectSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 2
                    tests[0].name shouldBe "a calculator"
                    tests[1].name shouldBe "addition works"
                }

                it("should find FreeSpec tests") {
                    val ktFile = KotlinParser.parse(KotestBlockExtractorTestFixture.freeSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 2
                    tests[0].name shouldBe "free spec container"
                    tests[1].name shouldBe "free spec leaf"
                }

                it("should find WordSpec tests") {
                    val ktFile = KotlinParser.parse(KotestBlockExtractorTestFixture.wordSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 5
                    tests[0].name shouldBe "word spec container"
                    tests[1].name shouldBe "word spec leaf"
                    tests[2].name shouldBe "word spec container When"
                    tests[3].name shouldBe "word spec container should"
                    tests[4].name shouldBe "word spec leaf nested"
                }

                it("should find AnnotationSpec tests") {
                    val ktFile = KotlinParser.parse(KotestBlockExtractorTestFixture.annotationSpec())
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    val tests = visitor.foundTests
                    tests.size shouldBe 1
                    tests[0].name shouldBe "annotation spec leaf"
                }
            }

            context("when parsing a class that does not extend Spec") {
                it("should not extract anything if not extending a Spec class") {
                    val code =
                        """
                        class SampleClass {
                            fun test() {
                                println("this is not a kotest test")
                            }
                        }
                        """.trimIndent()

                    val ktFile = KotlinParser.parse(code)
                    val visitor = KotestBlockExtractor()
                    ktFile.accept(visitor)

                    visitor.foundTests.size shouldBe 0
                }
            }
        }
    })
