package br.com.kotsniff.cli.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class NumericOptionsTest :
    FunSpec({
        test("precedence is CLI > file > default") {
            NumericOptions.resolveMaxAssertions(cliValue = 7, configValue = 9) shouldBe 7
            NumericOptions.resolveMaxAssertions(cliValue = null, configValue = 9) shouldBe 9
            NumericOptions.resolveMaxAssertions(cliValue = null, configValue = null) shouldBe NumericOptions.DEFAULT_MAX_ASSERTIONS

            NumericOptions.resolveReportMaxLines(cliValue = 50, configValue = 100) shouldBe 50
            NumericOptions.resolveReportMaxLines(cliValue = null, configValue = 100) shouldBe 100
            NumericOptions.resolveReportMaxLines(cliValue = null, configValue = null) shouldBe NumericOptions.DEFAULT_REPORT_MAX_LINES
        }

        test("valid boundary value (1) is accepted") {
            NumericOptions.resolveMaxAssertions(cliValue = 1, configValue = null) shouldBe 1
            NumericOptions.resolveReportMaxLines(cliValue = 1, configValue = null) shouldBe 1
        }

        test("negative values are rejected with an actionable message") {
            shouldThrow<IllegalArgumentException> {
                NumericOptions.resolveMaxAssertions(cliValue = -1, configValue = null)
            }.message shouldContain "maxAssertions"
            shouldThrow<IllegalArgumentException> {
                NumericOptions.resolveReportMaxLines(cliValue = -5, configValue = null)
            }.message shouldContain "reportMaxLines"
        }

        test("zero is rejected for both options") {
            shouldThrow<IllegalArgumentException> {
                NumericOptions.resolveMaxAssertions(cliValue = 0, configValue = null)
            }.message shouldContain "must be >= 1"
            shouldThrow<IllegalArgumentException> {
                NumericOptions.resolveReportMaxLines(cliValue = 0, configValue = null)
            }.message shouldContain "must be >= 1"
        }

        test("an invalid value from the file is rejected even when the CLI omits it") {
            shouldThrow<IllegalArgumentException> {
                NumericOptions.resolveMaxAssertions(cliValue = null, configValue = 0)
            }.message shouldContain "maxAssertions"
        }
    })
