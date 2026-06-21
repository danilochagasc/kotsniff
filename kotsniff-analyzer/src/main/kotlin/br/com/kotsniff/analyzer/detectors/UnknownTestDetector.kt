package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.isAssertion
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class UnknownTestDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        if (!testBlock.isTerminal) return emptyList()

        var assertionCount = 0

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    super.visitCallExpression(expression)
                    val callee = expression.calleeExpression
                    if (callee is KtNameReferenceExpression) {
                        val name = callee.getReferencedName()
                        if (name.isAssertion()) {
                            assertionCount++
                        }
                    }
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    super.visitBinaryExpression(expression)
                    val operation = expression.operationReference.text
                    if (operation.isAssertion()) {
                        assertionCount++
                    }
                }
            },
        )

        if (assertionCount == 0) {
            return listOf(
                DetectionResult(
                    smellType = TestSmell.UNKNOWN_TEST,
                    location = testBlock.originalExpression.toCodeLocation(filePath),
                    codeSnippet = testBlock.originalExpression.text,
                ),
            )
        }

        return emptyList()
    }
}
