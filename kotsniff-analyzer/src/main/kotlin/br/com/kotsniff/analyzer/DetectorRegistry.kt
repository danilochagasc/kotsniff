package br.com.kotsniff.analyzer

import br.com.kotsniff.analyzer.detectors.AssertionRouletteDetector
import br.com.kotsniff.analyzer.detectors.ConditionalTestLogicDetector
import br.com.kotsniff.analyzer.detectors.DuplicateAssertDetector
import br.com.kotsniff.analyzer.detectors.ExceptionHandlingDetector
import br.com.kotsniff.analyzer.detectors.IgnoredTestDetector
import br.com.kotsniff.analyzer.detectors.MagicNumberTestDetector
import br.com.kotsniff.analyzer.detectors.SleepyTestDetector
import br.com.kotsniff.analyzer.detectors.TestSmellDetector
import br.com.kotsniff.analyzer.detectors.UnknownTestDetector

/**
 * Central catalogue of detectors.
 *
 * Keeps rule construction and rule selection in one place, so adding a rule
 * requires registering it here and nothing else. [enabled] applies
 * [AnalysisSettings] (e.g. disabling rules) without changing the engine.
 */
class DetectorRegistry(
    private val detectors: List<TestSmellDetector>,
) {
    /** Detectors that are enabled given [settings]. */
    fun enabled(settings: AnalysisSettings): List<TestSmellDetector> = detectors.filter { it.id !in settings.disabledRules }

    /** All registered detectors, regardless of settings. */
    fun all(): List<TestSmellDetector> = detectors

    companion object {
        /** Builds the default set of detectors, parameterised by [settings]. */
        fun create(settings: AnalysisSettings): DetectorRegistry =
            DetectorRegistry(
                listOf(
                    IgnoredTestDetector(),
                    SleepyTestDetector(reportDelay = settings.reportDelay),
                    ConditionalTestLogicDetector(),
                    ExceptionHandlingDetector(),
                    UnknownTestDetector(),
                    DuplicateAssertDetector(),
                    MagicNumberTestDetector(settings.allowedNumbers),
                    AssertionRouletteDetector(settings.maxAssertions),
                ),
            )
    }
}
