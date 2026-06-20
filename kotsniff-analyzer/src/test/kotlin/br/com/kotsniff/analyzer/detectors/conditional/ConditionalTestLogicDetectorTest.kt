package br.com.kotsniff.analyzer.detectors.conditional

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.ConditionalTestLogicDetector
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.FILE_PATH_BEHAVIOR_SPEC
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.FILE_PATH_STRING_SPEC
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.conditionalBehaviorSpec
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.conditionalDescribeSpec
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.conditionalFunSpec
import br.com.kotsniff.analyzer.detectors.conditional.ConditionalTestLogicDetectorFixture.conditionalStringSpec
import br.com.kotsniff.core.enums.TestSmell.CONDITIONAL_TEST_LOGIC
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ConditionalTestLogicDetectorTest :
    DescribeSpec({
        describe("ConditionalTestLogicDetector") {
            val engine = KotSniffEngine(listOf(ConditionalTestLogicDetector()))

            it("should detect if in StringSpec") {
                val results = engine.analyze(conditionalStringSpec(), FILE_PATH_STRING_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe CONDITIONAL_TEST_LOGIC
                results.first().codeSnippet.startsWith("if (1 == 1)") shouldBe true
            }

            it("should detect when in FunSpec") {
                val results = engine.analyze(conditionalFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe CONDITIONAL_TEST_LOGIC
                results.first().codeSnippet.startsWith("when (1)") shouldBe true
            }

            it("should detect if in BehaviorSpec") {
                val results = engine.analyze(conditionalBehaviorSpec(), FILE_PATH_BEHAVIOR_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe CONDITIONAL_TEST_LOGIC
                results.first().codeSnippet.startsWith("if (true)") shouldBe true
            }

            it("should detect when in DescribeSpec") {
                val results = engine.analyze(conditionalDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe CONDITIONAL_TEST_LOGIC
                results.first().codeSnippet.startsWith("when (true)") shouldBe true
            }
        }
    })
