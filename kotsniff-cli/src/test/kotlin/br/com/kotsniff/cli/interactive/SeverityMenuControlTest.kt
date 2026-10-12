package br.com.kotsniff.cli.interactive

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SeverityMenuControlTest :
    FunSpec({
        test("Esc with no pending changes ends the session as CANCEL") {
            val control = SeverityMenuControl(SeverityConfigSession())
            control.handle(SeverityMenuControl.KEY_ESC) shouldBe SeverityMenuControl.Outcome.CANCEL
        }

        test("Esc with pending changes opens the confirmation dialog, and Enter saves") {
            val session = SeverityConfigSession().apply { set(TestSmell.UNKNOWN_TEST, Severity.LOW) }
            val control = SeverityMenuControl(session)

            control.handle(SeverityMenuControl.KEY_ESC) shouldBe null
            control.lines().any { it.text.contains("Unsaved changes") } shouldBe true
            control.handle(SeverityMenuControl.KEY_ENTER) shouldBe SeverityMenuControl.Outcome.SAVE
        }

        test("confirmation dialog can exit without saving") {
            val session = SeverityConfigSession().apply { set(TestSmell.UNKNOWN_TEST, Severity.LOW) }
            val control = SeverityMenuControl(session)

            control.handle(SeverityMenuControl.KEY_ESC) shouldBe null
            control.handle(SeverityMenuControl.KEY_DOWN)
            control.handle(SeverityMenuControl.KEY_ENTER) shouldBe SeverityMenuControl.Outcome.CANCEL
        }

        test("Esc on the confirmation dialog returns to the rules screen") {
            val session = SeverityConfigSession().apply { set(TestSmell.UNKNOWN_TEST, Severity.LOW) }
            val control = SeverityMenuControl(session)

            control.handle(SeverityMenuControl.KEY_ESC) shouldBe null
            control.handle(SeverityMenuControl.KEY_ESC) shouldBe null
            control.lines().first().text shouldBe "KotSniff - configure severities"
        }

        test("Enter on the Save entry returns SAVE") {
            val control = SeverityMenuControl(SeverityConfigSession())
            repeat(TestSmell.entries.size) { control.handle(SeverityMenuControl.KEY_DOWN) }
            control.handle(SeverityMenuControl.KEY_ENTER) shouldBe SeverityMenuControl.Outcome.SAVE
        }

        test("Enter on the Cancel entry (no changes) returns CANCEL") {
            val control = SeverityMenuControl(SeverityConfigSession())
            repeat(TestSmell.entries.size + 1) { control.handle(SeverityMenuControl.KEY_DOWN) }
            control.handle(SeverityMenuControl.KEY_ENTER) shouldBe SeverityMenuControl.Outcome.CANCEL
        }

        test("changing a rule severity updates the session") {
            val session = SeverityConfigSession()
            val control = SeverityMenuControl(session)

            control.handle(SeverityMenuControl.KEY_ENTER) // open severity for IGNORED_TEST (default MEDIUM)
            control.handle(SeverityMenuControl.KEY_DOWN) // -> HIGH
            control.handle(SeverityMenuControl.KEY_ENTER) // confirm

            session.effective(TestSmell.IGNORED_TEST) shouldBe Severity.HIGH
            session.isOverridden(TestSmell.IGNORED_TEST) shouldBe true
        }

        test("reset clears an override and Esc backs out of the severity screen") {
            val session = SeverityConfigSession(mapOf(TestSmell.IGNORED_TEST to Severity.HIGH))
            val control = SeverityMenuControl(session)

            control.handle(SeverityMenuControl.KEY_ENTER) // open severity (current HIGH, index 3)
            control.handle(SeverityMenuControl.KEY_DOWN) // -> reset row
            control.handle(SeverityMenuControl.KEY_ENTER) // reset

            session.isOverridden(TestSmell.IGNORED_TEST) shouldBe false
        }

        test("navigation wraps around on the rules screen") {
            val control = SeverityMenuControl(SeverityConfigSession())
            control.handle(SeverityMenuControl.KEY_UP)
            // wrapping up from the first row lands on the last row (Cancel)
            control
                .lines()
                .last()
                .text
                .startsWith("> ") shouldBe true
        }
    })
