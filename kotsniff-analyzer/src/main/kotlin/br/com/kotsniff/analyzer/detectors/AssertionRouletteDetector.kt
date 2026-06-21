package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.isAssertion
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class AssertionRouletteDetector(
    private val maxAssertions: Int = 3,
) : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        if (!testBlock.isTerminal) return emptyList()

        var assertionCount = 0

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    val calleeText = expression.calleeExpression?.text
                    if (calleeText.isAssertion()) {
                        assertionCount++
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    val operationText = expression.operationReference.text
                    if (operationText.isAssertion()) {
                        assertionCount++
                    }
                    super.visitBinaryExpression(expression)
                }
            },
        )

        if (assertionCount > maxAssertions) {
            return listOf(
                DetectionResult(
                    smellType = TestSmell.ASSERTION_ROULETTE,
                    location = testBlock.originalExpression.toCodeLocation(filePath),
                    codeSnippet = testBlock.originalExpression.text,
                ),
            )
        }

        return emptyList()
    }
}
