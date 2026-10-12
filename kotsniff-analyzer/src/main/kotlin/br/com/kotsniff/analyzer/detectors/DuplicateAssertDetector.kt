package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.isAssertionCall
import br.com.kotsniff.analyzer.utils.Assertions.isAssertionOperation
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.DUPLICATE_ASSERT
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Detects the exact same assertion repeated within one leaf test.
 *
 * Comparison is done on whitespace-normalised source text, so formatting
 * differences do not hide duplicates. It intentionally does not attempt to
 * detect *semantically* equivalent assertions (e.g. swapped arguments), which
 * would require type information.
 */
class DuplicateAssertDetector : TestSmellDetector {
    override val id = DUPLICATE_ASSERT

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val assertions = mutableListOf<String>()
        val results = mutableListOf<Diagnostic>()
        val duplicatesReported = mutableSetOf<String>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isAssertionCall()) {
                        val parent = expression.parent
                        val textToUse = if (parent is KtDotQualifiedExpression) parent.text else expression.text
                        val elementToLocate = if (parent is KtDotQualifiedExpression) parent else expression
                        checkAndAddAssertion(textToUse, elementToLocate)
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.isAssertionOperation()) {
                        checkAndAddAssertion(expression.text, expression)
                    }
                    super.visitBinaryExpression(expression)
                }

                private fun checkAndAddAssertion(
                    text: String,
                    element: KtElement,
                ) {
                    val normalizedText = text.replace(Regex("\\s+"), " ").trim()

                    if (assertions.contains(normalizedText)) {
                        if (!duplicatesReported.contains(normalizedText)) {
                            duplicatesReported.add(normalizedText)
                            results.add(
                                Diagnostic.of(
                                    DUPLICATE_ASSERT,
                                    element.toCodeLocation(filePath),
                                    text,
                                    message = "The same assertion is evaluated more than once in this test.",
                                    suggestion = "Remove the redundant assertion or assert a distinct property.",
                                ),
                            )
                        }
                    } else {
                        assertions.add(normalizedText)
                    }
                }
            },
        )

        return results
    }
}
