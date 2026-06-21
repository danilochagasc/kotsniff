package br.com.kotsniff.analyzer.detectors.exception

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.ExceptionHandlingDetector
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.FILE_PATH_BEHAVIOR_SPEC
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.FILE_PATH_STRING_SPEC
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.exceptionHandlingBehaviorSpec
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.exceptionHandlingDescribeSpec
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.exceptionHandlingFunSpec
import br.com.kotsniff.analyzer.detectors.exception.ExceptionHandlingDetectorFixture.exceptionHandlingStringSpec
import br.com.kotsniff.core.enums.TestSmell.EXCEPTION_HANDLING
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ExceptionHandlingDetectorTest :
    DescribeSpec({
        describe("ExceptionHandlingDetector") {
            val engine = KotSniffEngine(listOf(ExceptionHandlingDetector()))

            it("should detect try catch in StringSpec") {
                val results = engine.analyze(exceptionHandlingStringSpec(), FILE_PATH_STRING_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe EXCEPTION_HANDLING
                results.first().codeSnippet.startsWith("try {") shouldBe true
            }

            it("should detect try catch in FunSpec") {
                val results = engine.analyze(exceptionHandlingFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe EXCEPTION_HANDLING
                results.first().codeSnippet.startsWith("try {") shouldBe true
            }

            it("should detect try catch in BehaviorSpec") {
                val results = engine.analyze(exceptionHandlingBehaviorSpec(), FILE_PATH_BEHAVIOR_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe EXCEPTION_HANDLING
                results.first().codeSnippet.startsWith("try {") shouldBe true
            }

            it("should detect try catch in DescribeSpec") {
                val results = engine.analyze(exceptionHandlingDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe EXCEPTION_HANDLING
                results.first().codeSnippet.startsWith("try {") shouldBe true
            }
        }
    })
