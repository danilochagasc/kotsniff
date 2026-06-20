package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.IGNORED_TEST
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class IgnoredTestDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        val expr = testBlock.originalExpression

        val parentClass = expr.getParentOfType<KtClassOrObject>(true)
        val ignoreAnnotations = listOf("Ignored", "Disabled", "Ignore")
        val classAnnotation =
            parentClass?.annotationEntries?.firstOrNull {
                it.shortName?.asString() in ignoreAnnotations
            }

        if (classAnnotation != null) {
            return listOf(
                DetectionResult(
                    smellType = IGNORED_TEST,
                    location = classAnnotation.toCodeLocation(filePath),
                    codeSnippet = classAnnotation.text,
                ),
            )
        }

        val parent = expr.parent
        if (parent is KtPrefixExpression && parent.operationReference.text == "!") {
            return listOf(
                DetectionResult(
                    smellType = IGNORED_TEST,
                    location = parent.toCodeLocation(filePath),
                    codeSnippet = parent.text,
                ),
            )
        }

        val callee = expr.calleeExpression
        if (callee is KtNameReferenceExpression && callee.getReferencedName().startsWith("x")) {
            return listOf(
                DetectionResult(
                    smellType = IGNORED_TEST,
                    location = expr.toCodeLocation(filePath),
                    codeSnippet = expr.text,
                ),
            )
        }

        return emptyList()
    }
}
