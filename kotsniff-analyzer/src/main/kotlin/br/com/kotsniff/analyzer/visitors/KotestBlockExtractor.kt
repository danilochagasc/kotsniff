package br.com.kotsniff.analyzer.visitors

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * A PSI tree visitor that identifies Kotest test blocks across multiple Spec styles.
 *
 * Traverses the AST looking for classes that extend a `*Spec` super type,
 * then extracts test block names and bodies from within those classes.
 *
 * Supported patterns:
 * - **StringSpec**: `"test name" { body }` (string-invoke pattern)
 * - **FunSpec**: `test("name") { body }`, `context("name") { body }`
 * - **BehaviorSpec**: `given/when/then("name") { body }`
 * - **DescribeSpec**: `describe/context/it("name") { body }`
 */
class KotestBlockExtractor : KtTreeVisitorVoid() {
    private val _foundTests = mutableListOf<KotestBlock>()

    /** The list of test blocks discovered during traversal. */
    val foundTests: List<KotestBlock> get() = _foundTests.toList()

    private var specDepth = 0

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val isSpec =
            classOrObject.superTypeListEntries.any { entry ->
                entry.typeReference?.text?.endsWith("Spec") == true
            }

        if (isSpec) {
            specDepth++
            super.visitClassOrObject(classOrObject)
            specDepth--
        } else {
            super.visitClassOrObject(classOrObject)
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        if (specDepth > 0) {
            tryExtractTestBlock(expression)
        }
        super.visitCallExpression(expression)
    }

    private fun tryExtractTestBlock(expression: KtCallExpression) {
        val callee = expression.calleeExpression ?: return

        // The test body is always the trailing lambda: test("name") { body }
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return

        when (callee) {
            // StringSpec pattern: "test name" { body }
            is KtStringTemplateExpression -> {
                val name = callee.entries.joinToString("") { it.text }
                _foundTests.add(KotestBlock(name, lambda, expression))
            }

            // Named DSL functions: test("name") { }, given("name") { }, etc.
            is KtNameReferenceExpression -> {
                if (callee.getReferencedName() in KOTEST_DSL_NAMES) {
                    val name = extractFirstStringArgument(expression) ?: callee.getReferencedName()
                    _foundTests.add(KotestBlock(name, lambda, expression))
                }
            }
        }
    }

    private fun extractFirstStringArgument(expression: KtCallExpression): String? {
        val firstArg = expression.valueArguments.firstOrNull()?.getArgumentExpression()
        return (firstArg as? KtStringTemplateExpression)?.entries?.joinToString("") { it.text }
    }

    companion object {
        /** Known Kotest DSL function names that define test blocks or contexts. */
        private val KOTEST_DSL_NAMES =
            setOf(
                // FunSpec
                "test",
                "xtest",
                // BehaviorSpec
                "given",
                "Given",
                "xgiven",
                "when",
                "When",
                "xwhen",
                "then",
                "Then",
                "xthen",
                // DescribeSpec
                "describe",
                "xdescribe",
                "it",
                "xit",
                // Shared
                "context",
                "xcontext",
                // ShouldSpec
                "should",
                "xshould",
            )
    }
}
