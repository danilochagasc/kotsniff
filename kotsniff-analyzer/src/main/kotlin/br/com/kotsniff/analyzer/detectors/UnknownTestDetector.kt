package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.isAssertionCall
import br.com.kotsniff.analyzer.utils.Assertions.isAssertionOperation
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.UNKNOWN_TEST
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Detects leaf tests that contain neither assertions nor mock verifications.
 *
 * **Limitation:** assertions performed inside a helper function called from the
 * test are not visible without whole-program analysis, so such tests are
 * reported. This is a documented false positive (see the reference corpus).
 */
class UnknownTestDetector : TestSmellDetector {
    override val id = UNKNOWN_TEST

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        var assertionCount = 0

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isAssertionCall()) assertionCount++
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.isAssertionOperation()) assertionCount++
                    super.visitBinaryExpression(expression)
                }
            },
        )

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
