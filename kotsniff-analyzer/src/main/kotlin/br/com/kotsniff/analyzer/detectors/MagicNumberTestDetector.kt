package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.isAssertionCall
import br.com.kotsniff.analyzer.utils.Assertions.isAssertionOperation
import br.com.kotsniff.analyzer.utils.NumericLiterals
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.MAGIC_NUMBER_TEST
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Detects arbitrary numeric literals used inside assertions.
 *
 * Only literals that appear inside a recognised assertion are inspected, so
 * numeric arguments to unrelated helper calls are not reported. Values in
 * [allowedNumbers] (self-explanatory constants) are ignored.
 */
class MagicNumberTestDetector(
    private val allowedNumbers: Set<String> = NumericLiterals.DEFAULT_ALLOWED,
) : TestSmellDetector {
    override val id = MAGIC_NUMBER_TEST

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val results = mutableListOf<Diagnostic>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isAssertionCall()) {
                        inspectForMagicNumbers(expression, filePath, results)
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.isAssertionOperation()) {
                        inspectForMagicNumbers(expression, filePath, results)
                    }
                    super.visitBinaryExpression(expression)
                }
            },
        )

        return results
    }

    private fun inspectForMagicNumbers(
        expression: KtExpression,
        filePath: String,
        results: MutableList<Diagnostic>,
    ) {
        // Sub-traversal to find constants ONLY within the assertion expression
        expression.acceptChildren(
            object : KtTreeVisitorVoid() {
                override fun visitConstantExpression(constant: KtConstantExpression) {
                    val text = constant.text
                    if (NumericLiterals.isNumericLiteral(text) && NumericLiterals.normalize(text) !in allowedNumbers) {
                        results.add(
                            Diagnostic.of(
                                MAGIC_NUMBER_TEST,
                                constant.toCodeLocation(filePath),
                                text,
                                message = "Magic number '$text' used in an assertion.",
                                suggestion = "Extract the value into a well-named constant.",
                                metadata = mapOf("value" to text),
                            ),
                        )
                    }
                    super.visitConstantExpression(constant)
                }
            },
        )
    }
}
