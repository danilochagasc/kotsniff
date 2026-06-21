package br.com.kotsniff.analyzer.detectors.magicnumber

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.MagicNumberTestDetector
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.FILE_PATH_FREE_SPEC
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.FILE_PATH_INIT_SPEC
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.magicNumberDescribeSpec
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.magicNumberFreeSpec
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.magicNumberFunSpec
import br.com.kotsniff.analyzer.detectors.magicnumber.MagicNumberTestDetectorFixture.magicNumberInitSpec
import br.com.kotsniff.core.enums.TestSmell.MAGIC_NUMBER_TEST
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class MagicNumberTestDetectorTest :
    DescribeSpec({
        describe("MagicNumberTestDetector") {
            val engine = KotSniffEngine(listOf(MagicNumberTestDetector()))

            it("should detect magic numbers in FunSpec") {
                val results = engine.analyze(magicNumberFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe MAGIC_NUMBER_TEST
                results.first().codeSnippet shouldBe "404"
            }

            it("should detect magic numbers in DescribeSpec with floating points") {
                val results = engine.analyze(magicNumberDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe MAGIC_NUMBER_TEST
                results.first().codeSnippet shouldBe "10.5"
            }

            it("should detect magic numbers in FreeSpec with long suffix") {
                val results = engine.analyze(magicNumberFreeSpec(), FILE_PATH_FREE_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe MAGIC_NUMBER_TEST
                results.first().codeSnippet shouldBe "5000L"
            }

            it("should detect magic numbers when test is declared in init block") {
                val results = engine.analyze(magicNumberInitSpec(), FILE_PATH_INIT_SPEC)
                results shouldHaveSize 2
                results[0].smellType shouldBe MAGIC_NUMBER_TEST
                results[0].codeSnippet shouldBe "42"
                results[1].smellType shouldBe MAGIC_NUMBER_TEST
                results[1].codeSnippet shouldBe "42"
            }
        }
    })
