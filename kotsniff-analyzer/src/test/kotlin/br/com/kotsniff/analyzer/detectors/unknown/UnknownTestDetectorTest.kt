package br.com.kotsniff.analyzer.detectors.unknown

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.UnknownTestDetector
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.FILE_PATH_BEHAVIOR_SPEC
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.FILE_PATH_STRING_SPEC
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.unknownTestBehaviorSpec
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.unknownTestDescribeSpec
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.unknownTestFunSpec
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.unknownTestStringSpec
import br.com.kotsniff.analyzer.detectors.unknown.UnknownTestDetectorFixture.validTestWithMockkVerification
import br.com.kotsniff.core.enums.TestSmell.UNKNOWN_TEST
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class UnknownTestDetectorTest :
    DescribeSpec({
        describe("UnknownTestDetector") {
            val engine = KotSniffEngine(listOf(UnknownTestDetector()))

            it("should detect no assertions in StringSpec") {
                val results = engine.analyze(unknownTestStringSpec(), FILE_PATH_STRING_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe UNKNOWN_TEST
            }

            it("should detect no assertions in FunSpec") {
                val results = engine.analyze(unknownTestFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe UNKNOWN_TEST
            }

            it("should detect no assertions in BehaviorSpec") {
                val results = engine.analyze(unknownTestBehaviorSpec(), FILE_PATH_BEHAVIOR_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe UNKNOWN_TEST
            }

            it("should detect no assertions in DescribeSpec") {
                val results = engine.analyze(unknownTestDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe UNKNOWN_TEST
            }

            it("should NOT detect unknown test when using mock verification like coVerify") {
                val results = engine.analyze(validTestWithMockkVerification(), "SampleMockkSpec.kt")
                results shouldHaveSize 0
            }
        }
    })
