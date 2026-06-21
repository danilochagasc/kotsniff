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
import br.com.kotsniff.analyzer.parser.KotlinParser
import br.com.kotsniff.analyzer.visitors.KotestBlockExtractor
import br.com.kotsniff.core.models.DetectionResult

/**
 * Orchestrates the parsing of Kotlin code, extraction of Kotest blocks,
 * and the execution of Test Smell detectors.
 */
class KotSniffEngine(
    private val detectors: List<TestSmellDetector>,
) {
    /**
     * Analyzes a single Kotlin file's content and returns all detected test smells.
     *
     * @param code The source code to analyze.
     * @param filePath The path of the file (used for reporting locations).
     * @return A list of [DetectionResult] containing any found smells.
     */
    fun analyze(
        code: String,
        filePath: String,
    ): List<DetectionResult> {
        val normalizedCode = code.replace("\r\n", "\n")
        val ktFile = KotlinParser.parse(normalizedCode)
        val extractor = KotestBlockExtractor()

        // Extract test blocks
        ktFile.accept(extractor)

        // Run detectors against all found test blocks
        return extractor.foundTests
            .flatMap { testBlock ->
                detectors.flatMap { detector ->
                    detector.detect(testBlock, filePath)
                }
            }.distinctBy { result ->
                // Prevent duplicate reports for the same smell on the same line
                // caused by nested test blocks (e.g., given -> when -> then)
                "${result.smellType}-${result.location.line}-${result.location.column}"
            }
    }

    companion object {
        /**
         * Creates an engine with the default set of Test Smell Detectors.
         */
        fun createDefault(maxAssertions: Int = 3): KotSniffEngine {
            val detectors =
                listOf(
                    IgnoredTestDetector(),
                    SleepyTestDetector(),
                    ConditionalTestLogicDetector(),
                    ExceptionHandlingDetector(),
                    UnknownTestDetector(),
                    DuplicateAssertDetector(),
                    MagicNumberTestDetector(),
                    AssertionRouletteDetector(maxAssertions),
                )
            return KotSniffEngine(detectors)
        }
    }
}
