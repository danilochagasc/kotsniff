package br.com.kotsniff.analyzer.detectors.duplicate

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.DuplicateAssertDetector
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.FILE_PATH_FREE_SPEC
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.FILE_PATH_INIT_SPEC
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.duplicateAssertDescribeSpec
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.duplicateAssertFreeSpec
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.duplicateAssertFunSpec
import br.com.kotsniff.analyzer.detectors.duplicate.DuplicateAssertDetectorFixture.duplicateAssertInitSpec
import br.com.kotsniff.core.enums.TestSmell.DUPLICATE_ASSERT
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class DuplicateAssertDetectorTest :
    DescribeSpec({
        describe("DuplicateAssertDetector") {
            val engine = KotSniffEngine(listOf(DuplicateAssertDetector()))

            it("should detect duplicate infix assertions in FunSpec") {
                val results = engine.analyze(duplicateAssertFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().ruleId shouldBe DUPLICATE_ASSERT
                results.first().codeSnippet shouldBe "a shouldBe 1"
            }

            it("should detect duplicate function call assertions in DescribeSpec") {
                val results = engine.analyze(duplicateAssertDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 1
                results.first().ruleId shouldBe DUPLICATE_ASSERT
                results.first().codeSnippet shouldBe "assert(list.size == 2)"
            }

            it("should detect duplicate chained method assertions in FreeSpec") {
                val results = engine.analyze(duplicateAssertFreeSpec(), FILE_PATH_FREE_SPEC)
                results shouldHaveSize 1
                results.first().ruleId shouldBe DUPLICATE_ASSERT
                results.first().codeSnippet shouldBe "name.shouldBe(\"test\")"
            }

            it("should detect duplicate assertions when test is declared in init block") {
                val results = engine.analyze(duplicateAssertInitSpec(), FILE_PATH_INIT_SPEC)
                results shouldHaveSize 1
                results.first().ruleId shouldBe DUPLICATE_ASSERT
                // Due to parsing string equality and whitespaces, checking prefix
                results.first().codeSnippet.startsWith("expect(1 == 1)") shouldBe true
            }
        }
    })
