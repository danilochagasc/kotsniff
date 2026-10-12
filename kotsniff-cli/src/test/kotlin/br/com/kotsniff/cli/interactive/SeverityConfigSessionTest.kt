package br.com.kotsniff.cli.interactive

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

class SeverityConfigSessionTest :
    FunSpec({
        test("uses the default severity until overridden") {
            val session = SeverityConfigSession()
            session.effective(TestSmell.MAGIC_NUMBER_TEST) shouldBe TestSmell.MAGIC_NUMBER_TEST.defaultSeverity
            session.isOverridden(TestSmell.MAGIC_NUMBER_TEST) shouldBe false
        }

        test("setting a different severity records an override") {
            val session = SeverityConfigSession()
            session.set(TestSmell.MAGIC_NUMBER_TEST, Severity.HIGH)
            session.effective(TestSmell.MAGIC_NUMBER_TEST) shouldBe Severity.HIGH
            session.isOverridden(TestSmell.MAGIC_NUMBER_TEST) shouldBe true
            session.hasChanges shouldBe true
        }

        test("setting the default severity clears the override") {
            val session = SeverityConfigSession()
            session.set(TestSmell.MAGIC_NUMBER_TEST, Severity.HIGH)
            session.set(TestSmell.MAGIC_NUMBER_TEST, TestSmell.MAGIC_NUMBER_TEST.defaultSeverity)
            session.isOverridden(TestSmell.MAGIC_NUMBER_TEST) shouldBe false
            session.hasChanges shouldBe false
        }

        test("reset removes an override") {
            val session = SeverityConfigSession(mapOf(TestSmell.UNKNOWN_TEST to Severity.LOW))
            session.reset(TestSmell.UNKNOWN_TEST)
            session.hasChanges shouldBe true
            session.overrides.containsKey(TestSmell.UNKNOWN_TEST) shouldBe false
        }

        test("hasChanges compares against the initial state") {
            val session = SeverityConfigSession(mapOf(TestSmell.UNKNOWN_TEST to Severity.LOW))
            session.hasChanges shouldBe false
            session.set(TestSmell.UNKNOWN_TEST, Severity.HIGH)
            session.hasChanges shouldBe true
        }

        test("toConfigMap is sorted by rule id and uses labels") {
            val session = SeverityConfigSession()
            session.set(TestSmell.UNKNOWN_TEST, Severity.INFO)
            session.set(TestSmell.MAGIC_NUMBER_TEST, Severity.HIGH)
            session.toConfigMap() shouldContainExactly
                mapOf(
                    "MAGIC_NUMBER_TEST" to "high",
                    "UNKNOWN_TEST" to "info",
                )
        }
    })
