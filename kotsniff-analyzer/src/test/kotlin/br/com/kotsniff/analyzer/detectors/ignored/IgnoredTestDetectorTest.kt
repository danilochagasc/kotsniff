package br.com.kotsniff.analyzer.detectors.ignored

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.IgnoredTestDetector
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.FILE_PATH_BEHAVIOR_SPEC
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.FILE_PATH_CLASS_IGNORED
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.FILE_PATH_STRING_SPEC
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.classIgnoredSpec
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.ignoredBehaviorSpec
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.ignoredDescribeSpec
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.ignoredFunSpec
import br.com.kotsniff.analyzer.detectors.ignored.IgnoredTestDetectorFixture.ignoredStringSpec
import br.com.kotsniff.core.enums.TestSmell.IGNORED_TEST
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class IgnoredTestDetectorTest :
    DescribeSpec({
        describe("IgnoredTestDetector") {
            val engine = KotSniffEngine(listOf(IgnoredTestDetector()))

            it("should detect bang prefix in StringSpec") {
                val results = engine.analyze(ignoredStringSpec(), FILE_PATH_STRING_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe IGNORED_TEST
                results.first().codeSnippet.startsWith("\"!ignored string test\"") shouldBe true
            }

            it("should detect xtest in FunSpec") {
                val results = engine.analyze(ignoredFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe IGNORED_TEST
                results.first().codeSnippet.startsWith("xtest(\"ignored fun test\")") shouldBe true
            }

            it("should detect xgiven, xwhen, xthen in BehaviorSpec") {
                val results = engine.analyze(ignoredBehaviorSpec(), FILE_PATH_BEHAVIOR_SPEC)
                results shouldHaveSize 3

                results.count { it.codeSnippet.startsWith("xgiven(\"an ignored test\")") } shouldBe 1
                results.count { it.codeSnippet.startsWith("xwhen(\"an ignored when\")") } shouldBe 1
                results.count { it.codeSnippet.startsWith("xthen(\"an ignored then\")") } shouldBe 1
            }

            it("should detect xdescribe and xit in DescribeSpec") {
                val results = engine.analyze(ignoredDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 2

                results.count { it.codeSnippet.startsWith("xdescribe(\"an ignored describe\")") } shouldBe 1
                results.count { it.codeSnippet.startsWith("xit(\"an ignored it\")") } shouldBe 1
            }

            it("should detect class level ignore annotations and report only once") {
                val results =
                    engine.analyze(
                        classIgnoredSpec(),
                        FILE_PATH_CLASS_IGNORED,
                    )
                results shouldHaveSize 1
                results.first().smellType shouldBe IGNORED_TEST
                results.first().codeSnippet shouldBe "@Ignored"
            }

            it("should detect class level ignore annotations with init blocks") {
                val results =
                    engine.analyze(
                        IgnoredTestDetectorFixture.classIgnoredSpecWithInit(),
                        IgnoredTestDetectorFixture.FILE_PATH_CLASS_IGNORED_INIT,
                    )
                results shouldHaveSize 1
                results.first().smellType shouldBe IGNORED_TEST
                results.first().codeSnippet shouldBe "@Ignored"
            }
        }
    })
