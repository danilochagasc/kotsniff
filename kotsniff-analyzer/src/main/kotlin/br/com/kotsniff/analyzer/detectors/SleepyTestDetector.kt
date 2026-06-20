package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.enums.TestSmell.SLEEPY_TEST
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class SleepyTestDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitExpression(expression: org.jetbrains.kotlin.psi.KtExpression) {
                    super.visitExpression(expression)

                    if (expression is KtCallExpression) {
                        val callee = expression.calleeExpression
                        if (callee is KtNameReferenceExpression && callee.getReferencedName() == "delay") {
                            results.add(
                                DetectionResult(
                                    smellType = SLEEPY_TEST,
                                    location = expression.toCodeLocation(filePath),
                                    codeSnippet = expression.text,
                                ),
                            )
                        }
                    } else if (expression is org.jetbrains.kotlin.psi.KtDotQualifiedExpression) {
                        if (expression.text.startsWith("Thread.sleep")) {
                            results.add(
                                DetectionResult(
                                    smellType = SLEEPY_TEST,
                                    location = expression.toCodeLocation(filePath),
                                    codeSnippet = expression.text,
                                ),
                            )
                        }
                    }
                }
            },
        )

        return results
    }
}
