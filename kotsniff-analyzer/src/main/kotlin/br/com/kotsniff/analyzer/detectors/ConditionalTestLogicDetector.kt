package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.CONDITIONAL_TEST_LOGIC
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression

class ConditionalTestLogicDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitIfExpression(expression: KtIfExpression) {
                    super.visitIfExpression(expression)
                    results.add(
                        DetectionResult(
                            smellType = CONDITIONAL_TEST_LOGIC,
                            location = expression.toCodeLocation(filePath),
                            codeSnippet = expression.text,
                        ),
                    )
                }

                override fun visitWhenExpression(expression: KtWhenExpression) {
                    super.visitWhenExpression(expression)
                    results.add(
                        DetectionResult(
                            smellType = CONDITIONAL_TEST_LOGIC,
                            location = expression.toCodeLocation(filePath),
                            codeSnippet = expression.text,
                        ),
                    )
                }
            },
        )

        return results
    }
}
