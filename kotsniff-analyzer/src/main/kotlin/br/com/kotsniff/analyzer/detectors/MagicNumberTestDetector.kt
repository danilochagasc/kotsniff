package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.isAssertion
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.DetectionResult
import br.com.kotsniff.core.models.CodeLocation
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class MagicNumberTestDetector : TestSmellDetector {
    private val allowedNumbers = setOf("0", "1", "-1", "100")

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        if (!testBlock.isTerminal) return emptyList()

        val results = mutableListOf<DetectionResult>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    val calleeText = expression.calleeExpression?.text
                    if (calleeText.isAssertion()) {
                        inspectForMagicNumbers(expression)
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    val operationText = expression.operationReference.text
                    if (operationText.isAssertion()) {
                        inspectForMagicNumbers(expression)
                    }
                    super.visitBinaryExpression(expression)
                }

                private fun inspectForMagicNumbers(expression: KtExpression) {
                    // Sub-traversal to find constants ONLY within the assertion expression
                    expression.acceptChildren(
                        object : KtTreeVisitorVoid() {
                            override fun visitConstantExpression(constant: KtConstantExpression) {
                                val text = constant.text
                                // Remove L and F suffixes for parsing check
                                val numericText = text.replace(Regex("[fFlL]"), "")
                                val isNumber = numericText.toDoubleOrNull() != null
                                
                                if (isNumber && !allowedNumbers.contains(text)) {
                                    results.add(
                                        DetectionResult(
                                            smellType = TestSmell.MAGIC_NUMBER_TEST,
                                            location = constant.toCodeLocation(filePath),
                                            codeSnippet = constant.text
                                        )
                                    )
                                }
                                super.visitConstantExpression(constant)
                            }
                        }
                    )
                }
            },
        )

        return results
    }
}
