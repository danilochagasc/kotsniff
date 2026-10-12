package br.com.kotsniff.analyzer.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NumericLiteralsTest :
    FunSpec({
        test("normalizes underscores and type suffixes") {
            NumericLiterals.normalize("1_000") shouldBe "1000"
            NumericLiterals.normalize("5000L") shouldBe "5000"
            NumericLiterals.normalize("10.5f") shouldBe "10.5"
        }

        test("recognizes decimal, floating, long and hexadecimal literals") {
            NumericLiterals.isNumericLiteral("404") shouldBe true
            NumericLiterals.isNumericLiteral("10.5") shouldBe true
            NumericLiterals.isNumericLiteral("5000L") shouldBe true
            NumericLiterals.isNumericLiteral("1_000") shouldBe true
            NumericLiterals.isNumericLiteral("0xFF") shouldBe true
            NumericLiterals.isNumericLiteral("0b1010") shouldBe true
        }

        test("rejects non-numeric text") {
            NumericLiterals.isNumericLiteral("hello") shouldBe false
            NumericLiterals.isNumericLiteral("\"text\"") shouldBe false
            NumericLiterals.isNumericLiteral("") shouldBe false
        }
    })
