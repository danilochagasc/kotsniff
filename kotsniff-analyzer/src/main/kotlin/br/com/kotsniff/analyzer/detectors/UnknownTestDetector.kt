package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.collectAssertionUnits
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.UNKNOWN_TEST
import br.com.kotsniff.core.models.Diagnostic

/**
 * Detects leaf tests that contain neither assertions nor mock verifications.
 *
 * Assertion counting uses the shared [collectAssertionUnits] policy. **Known
 * limitations** (see README): assertions performed inside a helper function
 * called from the test are not visible without whole-program analysis, and a
 * lambda argument is assumed to execute. Both can cause false positives.
 */
class UnknownTestDetector : TestSmellDetector {
    override val id = UNKNOWN_TEST

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val assertionCount = collectAssertionUnits(testBlock.body).size

        if (assertionCount == 0) {
            return listOf(
                Diagnostic.of(
                    UNKNOWN_TEST,
                    testBlock.originalExpression.toCodeLocation(filePath),
                    testBlock.originalExpression.text,
                    message = "Test '${testBlock.name}' has no assertions or mock verifications.",
                    suggestion = "Add an assertion or verify a mock interaction.",
                    metadata = mapOf("assertionCount" to "0"),
                ),
            )
        }

        return emptyList()
    }
}
