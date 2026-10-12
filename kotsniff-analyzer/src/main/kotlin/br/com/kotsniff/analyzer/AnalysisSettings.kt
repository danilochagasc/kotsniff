package br.com.kotsniff.analyzer

import br.com.kotsniff.analyzer.utils.NumericLiterals
import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell

/**
 * User-facing configuration for a single analysis run.
 *
 * @property disabledRules Rules that should not run.
 * @property maxAssertions Maximum assertions allowed per test block before
 *   `ASSERTION_ROULETTE` is reported.
 * @property reportDelay Whether `delay(...)` calls are reported as `SLEEPY_TEST`.
 *   Disabled by default because `delay` is common in legitimate coroutine tests.
 * @property allowedNumbers The effective allowlist of numeric literals reported
 *   as *not* magic. Defaults to [NumericLiterals.DEFAULT_ALLOWED]. The CLI
 *   composes it by adding user-provided values (`--allowed-number` / config) to
 *   the defaults.
 * @property severityOverrides Per-rule severity overrides. Rules absent from the
 *   map keep their [TestSmell.defaultSeverity].
 */
data class AnalysisSettings(
    val disabledRules: Set<TestSmell> = emptySet(),
    val maxAssertions: Int = 3,
    val reportDelay: Boolean = false,
    val allowedNumbers: Set<String> = NumericLiterals.DEFAULT_ALLOWED,
    val severityOverrides: Map<TestSmell, Severity> = emptyMap(),
)
