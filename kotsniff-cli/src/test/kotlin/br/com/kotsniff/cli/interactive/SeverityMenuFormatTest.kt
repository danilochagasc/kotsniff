package br.com.kotsniff.cli.interactive

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SeverityMenuFormatTest :
    FunSpec({
        test("rule line is a single line with id, severity and origin") {
            val line = SeverityMenuFormat.ruleLine(TestSmell.UNKNOWN_TEST, Severity.LOW, overridden = true)
            line shouldNotContain "\n"
            line shouldContain "Unknown Test"
            line shouldContain "UNKNOWN_TEST"
            line shouldContain "low"
            line shouldContain "override"
        }

        test("rule line marks the default origin when not overridden") {
            SeverityMenuFormat.ruleLine(TestSmell.MAGIC_NUMBER_TEST, Severity.LOW, overridden = false) shouldContain "default"
        }

        test("severity line marks the current and default options") {
            SeverityMenuFormat.severityLine(Severity.MEDIUM, current = true, isDefault = false) shouldBe "[x] medium"
            SeverityMenuFormat.severityLine(Severity.LOW, current = false, isDefault = true) shouldBe "[ ] low  (default)"
        }
    })
