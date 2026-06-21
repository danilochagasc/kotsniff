package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.isAssertion
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.DetectionResult
import br.com.kotsniff.core.models.CodeLocation
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class DuplicateAssertDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        if (!testBlock.isTerminal) return emptyList()

        val assertions = mutableListOf<String>()
        val results = mutableListOf<DetectionResult>()
        val duplicatesReported = mutableSetOf<String>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    val calleeText = expression.calleeExpression?.text
                    if (calleeText.isAssertion()) {
                        val parent = expression.parent
                        val textToUse = if (parent is KtDotQualifiedExpression) parent.text else expression.text
                        val elementToLocate = if (parent is KtDotQualifiedExpression) parent else expression
                        checkAndAddAssertion(textToUse, elementToLocate)
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    val operationText = expression.operationReference.text
                    if (operationText.isAssertion()) {
                        checkAndAddAssertion(expression.text, expression)
                    }
                    super.visitBinaryExpression(expression)
                }

                private fun checkAndAddAssertion(text: String, element: KtElement) {
                    val normalizedText = text.replace(Regex("\\s+"), " ").trim()
                    
                    if (assertions.contains(normalizedText)) {
                        if (!duplicatesReported.contains(normalizedText)) {
                            results.add(
                                DetectionResult(
                                    smellType = TestSmell.DUPLICATE_ASSERT,
                                    location = element.toCodeLocation(filePath),
                                    codeSnippet = text
                                )
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
