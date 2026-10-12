package br.com.kotsniff.analyzer

import br.com.kotsniff.analyzer.detectors.TestSmellDetector
import br.com.kotsniff.analyzer.parser.KotlinParser
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractor
import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.Diagnostic

/**
 * The outcome of analysing a single file.
 *
 * @property diagnostics The detected smells.
 * @property hasSyntaxErrors Whether the file could not be fully parsed.
 */
data class FileAnalysisResult(
    val diagnostics: List<Diagnostic>,
    val hasSyntaxErrors: Boolean,
)

/**
 * Orchestrates the parsing of Kotlin code, extraction of Kotest blocks,
 * and the execution of test smell detectors.
 *
 * @property severityOverrides Per-rule severities applied to every diagnostic
 *   (rules absent from the map keep the severity assigned by the detector).
 */
class KotSniffEngine(
    private val detectors: List<TestSmellDetector>,
    private val severityOverrides: Map<TestSmell, Severity> = emptyMap(),
) {
    /**
     * Analyzes a single Kotlin file's content and returns all detected test smells.
     *
     * @param code The source code to analyze.
     * @param filePath The path of the file (used for reporting locations).
     * @return A list of [Diagnostic] containing any found smells.
     */
    fun analyze(
        code: String,
        filePath: String,
    ): List<Diagnostic> = analyzeDetailed(code, filePath).diagnostics

    /**
     * Like [analyze] but also reports whether the file contained syntax errors,
     * so callers (e.g. the CLI runner) can mark it as failed.
     */
    fun analyzeDetailed(
        code: String,
        filePath: String,
    ): FileAnalysisResult {
        val normalizedCode = code.removePrefix("\uFEFF").replace("\r\n", "\n")
        val ktFile = KotlinParser.parse(normalizedCode)
        val hasSyntaxErrors = KotlinParser.hasSyntaxErrors(ktFile)

        // A malformed file cannot be analysed reliably: report it as failed
        // instead of emitting diagnostics derived from a partial PSI tree.
        if (hasSyntaxErrors) {
            return FileAnalysisResult(emptyList(), hasSyntaxErrors = true)
        }

        val extractor = KotestBlockExtractor()

        // Extract test blocks
        ktFile.accept(extractor)

        // Run detectors against all found test blocks
        val diagnostics =
            extractor.foundTests
                .flatMap { testBlock ->
                    detectors.flatMap { detector ->
                        detector.detect(testBlock, filePath)
                    }
                }.map { diagnostic ->
                    // Apply per-rule severity overrides (defaults when not configured).
                    severityOverrides[diagnostic.ruleId]?.let { diagnostic.copy(severity = it) } ?: diagnostic
                }.distinctBy { result ->
                    // Prevent duplicate reports for the same smell on the same line/column
                    // caused by nested test blocks (e.g., given -> when -> then)
                    "${result.ruleId}-${result.location.line}-${result.location.column}"
                }

        return FileAnalysisResult(diagnostics, hasSyntaxErrors)
    }

    companion object {
        /**
         * Creates an engine with the default set of Test Smell Detectors.
         */
        fun createDefault(settings: AnalysisSettings = AnalysisSettings()): KotSniffEngine =
            KotSniffEngine(
                DetectorRegistry.create(settings).enabled(settings),
                severityOverrides = settings.severityOverrides,
            )
    }
}
