package br.com.kotsniff.analyzer.detectors.roulette

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.AssertionRouletteDetector
import br.com.kotsniff.analyzer.detectors.roulette.AssertionRouletteDetectorFixture.FILE_PATH_ROULETTE_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.roulette.AssertionRouletteDetectorFixture.FILE_PATH_ROULETTE_INIT_SPEC
import br.com.kotsniff.analyzer.detectors.roulette.AssertionRouletteDetectorFixture.FILE_PATH_ROULETTE_STRING_SPEC
import br.com.kotsniff.analyzer.detectors.roulette.AssertionRouletteDetectorFixture.rouletteDescribeSpec
import br.com.kotsniff.analyzer.detectors.roulette.AssertionRouletteDetectorFixture.rouletteInitSpec
import br.com.kotsniff.analyzer.detectors.roulette.AssertionRouletteDetectorFixture.rouletteStringSpec
import br.com.kotsniff.core.enums.TestSmell.ASSERTION_ROULETTE
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class AssertionRouletteDetectorTest :
    DescribeSpec({

        describe("AssertionRouletteDetector") {
            it("should detect assertion roulette with default threshold (3) when block has 4 assertions in DescribeSpec") {
                val engine = KotSniffEngine(listOf(AssertionRouletteDetector()))
                val results = engine.analyze(rouletteDescribeSpec(), FILE_PATH_ROULETTE_DESCRIBE_SPEC)

                // Should find 1 smell for the 'it' block with 4 assertions, but ignore the 'it' block with 2.
                results shouldHaveSize 1
                results.first().smellType shouldBe ASSERTION_ROULETTE
            }

            it("should detect assertion roulette with default threshold (3) when block has 4 assertions in StringSpec") {
                val engine = KotSniffEngine(listOf(AssertionRouletteDetector()))
                val results = engine.analyze(rouletteStringSpec(), FILE_PATH_ROULETTE_STRING_SPEC)

                results shouldHaveSize 1
                results.first().smellType shouldBe ASSERTION_ROULETTE
            }

            it("should detect assertion roulette with default threshold (3) when block has 4 assertions in init block") {
                val engine = KotSniffEngine(listOf(AssertionRouletteDetector()))
                val results = engine.analyze(rouletteInitSpec(), FILE_PATH_ROULETTE_INIT_SPEC)

                results shouldHaveSize 1
                results.first().smellType shouldBe ASSERTION_ROULETTE
            }

            it("should NOT detect assertion roulette when configured with maxAssertions = 5 and block has 4 assertions") {
                // Explicitly configuring the detector with a higher threshold
                val engine = KotSniffEngine(listOf(AssertionRouletteDetector(maxAssertions = 5)))

                val resultsDescribe = engine.analyze(rouletteDescribeSpec(), FILE_PATH_ROULETTE_DESCRIBE_SPEC)
                resultsDescribe shouldHaveSize 0 // 4 assertions <= 5

                val resultsString = engine.analyze(rouletteStringSpec(), FILE_PATH_ROULETTE_STRING_SPEC)
                resultsString shouldHaveSize 0 // 4 assertions <= 5

                val resultsInit = engine.analyze(rouletteInitSpec(), FILE_PATH_ROULETTE_INIT_SPEC)
                resultsInit shouldHaveSize 0 // 4 assertions <= 5
            }
        }
    })
