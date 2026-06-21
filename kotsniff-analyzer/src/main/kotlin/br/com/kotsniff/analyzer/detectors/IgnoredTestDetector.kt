package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.IGNORED_TEST
import br.com.kotsniff.core.models.DetectionResult
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Detects ignored/disabled tests using three strategies:
 *
 * 1. **Bang prefix**: Test name starts with `!` (e.g., `test("!disabled") { }`)
 * 2. **X-prefix DSL**: Functions like `xtest`, `xdescribe`, `xit`, etc.
 * 3. **Class-level annotations**: `@Ignored`, `@Disabled`, `@Ignore` on the Spec class.
 */
class IgnoredTestDetector : TestSmellDetector {
    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult> {
        val expr = testBlock.originalExpression

        val parentClass = expr.getParentOfType<KtClassOrObject>(true)
        val classAnnotation =
            parentClass?.annotationEntries?.firstOrNull { annotation ->
                annotation.shortName?.asString() in IGNORE_ANNOTATIONS
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

        // 2. Bang prefix: test name starts with "!"
        if (testBlock.name.startsWith("!")) {
            return listOf(
                DetectionResult(
                    smellType = IGNORED_TEST,
                    location = expr.toCodeLocation(filePath),
                    codeSnippet = expr.text,
                ),
            )
        }

        // 3. X-prefix DSL functions (xtest, xdescribe, xit, etc.)
        val callee = (expr as? org.jetbrains.kotlin.psi.KtCallExpression)?.calleeExpression
        if (callee is KtNameReferenceExpression && callee.getReferencedName().startsWith("x")) {
            return listOf(
                DetectionResult(
                    smellType = IGNORED_TEST,
                    location = expr.toCodeLocation(filePath),
                    codeSnippet = expr.text,
                ),
            )
        }

        // 4. Function-level annotations (for AnnotationSpec)
        if (expr is org.jetbrains.kotlin.psi.KtNamedFunction) {
            val hasIgnoreAnnotation = expr.annotationEntries.any { it.shortName?.asString() in IGNORE_ANNOTATIONS }
            if (hasIgnoreAnnotation) {
                return listOf(
                    DetectionResult(
                        smellType = IGNORED_TEST,
                        location = expr.toCodeLocation(filePath),
                        codeSnippet = expr.text,
                    ),
                )
            }
        }

        return emptyList()
    }

    companion object {
        private val IGNORE_ANNOTATIONS = listOf("Ignored", "Disabled", "Ignore")
    }
}
