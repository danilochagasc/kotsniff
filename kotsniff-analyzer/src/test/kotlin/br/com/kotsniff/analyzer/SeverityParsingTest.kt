package br.com.kotsniff.analyzer

import br.com.kotsniff.core.enums.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SeverityParsingTest :
    FunSpec({
        test("parses severities case-insensitively, by name or label") {
            Severity.fromId("HIGH") shouldBe Severity.HIGH
            Severity.fromId("high") shouldBe Severity.HIGH
            Severity.fromId(" Medium ") shouldBe Severity.MEDIUM
            Severity.fromId("low") shouldBe Severity.LOW
        }

        test("returns null for unknown values") {
            Severity.fromId("critical") shouldBe null
            Severity.fromId("") shouldBe null
        }
    })
