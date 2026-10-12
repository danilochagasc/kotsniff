package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.IGNORED_TEST
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Detects ignored/disabled tests using four strategies:
 *
 * 1. **Class-level annotations**: `@Ignored`, `@Disabled`, `@Ignore` on the Spec class.
 * 2. **Bang prefix**: Test name starts with `!` (e.g., `test("!disabled") { }`)
 * 3. **X-prefix DSL**: Functions like `xtest`, `xdescribe`, `xit`, etc.
 * 4. **Function-level annotations**: `@Ignored`/`@Disabled`/`@Ignore` on a test method.
 */
class IgnoredTestDetector : TestSmellDetector {
    override val id = IGNORED_TEST

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        val expr = testBlock.originalExpression

        val parentClass = expr.getParentOfType<KtClassOrObject>(true)
        val classAnnotation =
            parentClass?.annotationEntries?.firstOrNull { annotation ->
                annotation.shortName?.asString() in IGNORE_ANNOTATIONS
            }

        if (classAnnotation != null) {
            return listOf(
                Diagnostic.of(
                    IGNORED_TEST,
                    classAnnotation.toCodeLocation(filePath),
                    classAnnotation.text,
                    message = "The whole test spec is disabled by @${classAnnotation.shortName?.asString()}.",
                    suggestion = "Re-enable the test if it should run, or remove it if obsolete.",
                ),
            )
        }

        // Bang prefix: test name starts with "!"
        if (testBlock.name.startsWith("!")) {
            return listOf(
                Diagnostic.of(
                    IGNORED_TEST,
                    expr.toCodeLocation(filePath),
                    expr.text,
                    message = "Test '${testBlock.name}' is disabled with the '!' prefix.",
                    suggestion = "Re-enable the test if it should run, or remove it if obsolete.",
                ),
            )
        }

        // X-prefix DSL functions (xtest, xdescribe, xit, etc.)
        val callee = (expr as? KtCallExpression)?.calleeExpression
        if (callee is KtNameReferenceExpression && callee.getReferencedName().startsWith("x")) {
            return listOf(
                Diagnostic.of(
                    IGNORED_TEST,
                    expr.toCodeLocation(filePath),
                    expr.text,
                    message = "Test is disabled via the '${callee.getReferencedName()}' DSL.",
                    suggestion = "Re-enable the test if it should run, or remove it if obsolete.",
                ),
            )
        }

        // Function-level annotations (for AnnotationSpec)
        if (expr is KtNamedFunction) {
            val annotation = expr.annotationEntries.firstOrNull { it.shortName?.asString() in IGNORE_ANNOTATIONS }
            if (annotation != null) {
                return listOf(
                    Diagnostic.of(
                        IGNORED_TEST,
                        annotation.toCodeLocation(filePath),
                        annotation.text,
                        message = "Test is disabled by @${annotation.shortName?.asString()}.",
                        suggestion = "Re-enable the test if it should run, or remove it if obsolete.",
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
