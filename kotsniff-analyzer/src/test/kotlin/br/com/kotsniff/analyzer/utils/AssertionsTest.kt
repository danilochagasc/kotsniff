package br.com.kotsniff.analyzer.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AssertionsTest :
    FunSpec({
        test("recognizes Kotest matchers by their should prefix") {
            Assertions.isMatcherName("shouldBe") shouldBe true
            Assertions.isMatcherName("shouldNotBe") shouldBe true
            Assertions.isAssertionFunction("shouldContain") shouldBe true
        }

        test("recognizes known assertion helpers") {
            Assertions.isAssertionFunction("assert") shouldBe true
            Assertions.isAssertionFunction("assertFailsWith") shouldBe true
            Assertions.isAssertionFunction("expect") shouldBe true
        }

        test("recognizes the whole MockK verification vocabulary") {
            listOf(
                "verify",
                "coVerify",
                "verifyOrder",
                "coVerifyOrder",
                "verifySequence",
                "coVerifySequence",
                "verifyAll",
                "confirmVerified",
            ).forEach { Assertions.isAssertionFunction(it) shouldBe true }
        }

        test("does not treat domain names as assertions") {
            Assertions.isAssertionFunction("expectStatus") shouldBe false
            Assertions.isAssertionFunction("expected") shouldBe false
            Assertions.isAssertionFunction("verifyInput") shouldBe false
            Assertions.isAssertionFunction("assertEqualsDomain") shouldBe false
        }

        test("does not treat stubbing as an assertion") {
            Assertions.isAssertionFunction("every") shouldBe false
            Assertions.isAssertionFunction("coEvery") shouldBe false
            Assertions.isStubbingFunction("every") shouldBe true
            Assertions.isStubbingFunction("coEvery") shouldBe true
        }
    })
