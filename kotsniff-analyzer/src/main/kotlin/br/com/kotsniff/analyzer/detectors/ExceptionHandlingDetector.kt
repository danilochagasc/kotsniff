package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression

class ExceptionHandlingDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitTryExpression(expression: KtTryExpression) {
                    super.visitTryExpression(expression)

                    results.add(
                        DetectionResult(
                            smellType = TestSmell.EXCEPTION_HANDLING,
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
