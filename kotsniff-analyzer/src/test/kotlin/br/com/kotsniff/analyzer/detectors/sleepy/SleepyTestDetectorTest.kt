package br.com.kotsniff.analyzer.detectors.sleepy

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.detectors.SleepyTestDetector
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.FILE_PATH_BEHAVIOR_SPEC
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.FILE_PATH_DESCRIBE_SPEC
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.FILE_PATH_FUN_SPEC
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.FILE_PATH_STRING_SPEC
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.sleepyBehaviorSpec
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.sleepyDescribeSpec
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.sleepyFunSpec
import br.com.kotsniff.analyzer.detectors.sleepy.SleepyTestDetectorFixture.sleepyStringSpec
import br.com.kotsniff.core.enums.TestSmell.SLEEPY_TEST
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SleepyTestDetectorTest :
    DescribeSpec({
        describe("SleepyTestDetector") {
            val engine = KotSniffEngine(listOf(SleepyTestDetector()))

            it("should detect Thread.sleep in StringSpec") {
                val results = engine.analyze(sleepyStringSpec(), FILE_PATH_STRING_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe SLEEPY_TEST
                results.first().codeSnippet shouldBe "Thread.sleep(1000)"
            }

            it("should detect delay in FunSpec") {
                val results = engine.analyze(sleepyFunSpec(), FILE_PATH_FUN_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe SLEEPY_TEST
                results.first().codeSnippet shouldBe "delay(500L)"
            }

            it("should detect Thread.sleep in BehaviorSpec") {
                val results = engine.analyze(sleepyBehaviorSpec(), FILE_PATH_BEHAVIOR_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe SLEEPY_TEST
                results.first().codeSnippet shouldBe "Thread.sleep(500)"
            }

            it("should detect delay in DescribeSpec") {
                val results = engine.analyze(sleepyDescribeSpec(), FILE_PATH_DESCRIBE_SPEC)
                results shouldHaveSize 1
                results.first().smellType shouldBe SLEEPY_TEST
                results.first().codeSnippet shouldBe "delay(100L)"
            }
        }
    })
