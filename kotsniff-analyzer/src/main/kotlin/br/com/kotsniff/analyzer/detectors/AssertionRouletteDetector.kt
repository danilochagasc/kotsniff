package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.collectAssertionUnits
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.ASSERTION_ROULETTE
import br.com.kotsniff.core.models.Diagnostic

/**
 * Detects tests with too many assertions in a single block.
 *
 * When a test has many assertions, a failure message rarely points to the
 * specific broken expectation, forcing the developer to debug the whole test.
 *
 * Assertions are counted with the shared [collectAssertionUnits] policy: each
 * assertion call/infix matcher is one unit, and nested expressions (including
 * compound helpers such as `assertSoftly { ... }`) are not counted again.
 */
class AssertionRouletteDetector(
    private val maxAssertions: Int = 3,
) : TestSmellDetector {
    override val id = ASSERTION_ROULETTE

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val assertionCount = collectAssertionUnits(testBlock.body).size

        if (assertionCount > maxAssertions) {
            return listOf(
                Diagnostic.of(
                    ASSERTION_ROULETTE,
                    testBlock.originalExpression.toCodeLocation(filePath),
                    testBlock.originalExpression.text,
                    message = "Test block has $assertionCount assertions (maximum allowed is $maxAssertions).",
                    suggestion = "Split the test into focused tests, or assert with contextual messages.",
                    metadata =
                        mapOf(
                            "assertionCount" to assertionCount.toString(),
                            "maxAssertions" to maxAssertions.toString(),
                        ),
                ),
            )
        }

        return emptyList()
    }
}
