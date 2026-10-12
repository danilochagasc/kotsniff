package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.CONDITIONAL_TEST_LOGIC
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression

/**
 * Detects control-flow conditionals (`if`/`when`) inside leaf test blocks.
 *
 * Only **statement-position** conditionals are reported: an `if`/`when` used as
 * an expression to build a value (e.g. a local helper argument) is a common and
 * legitimate pattern and would otherwise be a false positive. Container blocks
 * are not inspected to avoid double reporting.
 */
class ConditionalTestLogicDetector : TestSmellDetector {
    override val id = CONDITIONAL_TEST_LOGIC

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val results = mutableListOf<Diagnostic>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitIfExpression(expression: KtIfExpression) {
                    if (isStatementPosition(expression)) {
                        results.add(diagnostic(expression, filePath))
                    }
                    super.visitIfExpression(expression)
                }

                override fun visitWhenExpression(expression: KtWhenExpression) {
                    if (isStatementPosition(expression)) {
                        results.add(diagnostic(expression, filePath))
                    }
                    super.visitWhenExpression(expression)
                }
            },
        )

        return results
    }

    private fun diagnostic(
        expression: KtExpression,
        filePath: String,
    ): Diagnostic =
        Diagnostic.of(
            CONDITIONAL_TEST_LOGIC,
            expression.toCodeLocation(filePath),
            expression.text,
            message = "Test contains control-flow logic (${expression.node.elementType}).",
            suggestion = "Prefer a single execution path per test; move branching into the production code or split the test.",
        )

    private fun isStatementPosition(expression: KtExpression): Boolean = expression.parent is KtBlockExpression
}
