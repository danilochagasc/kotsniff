package br.com.kotsniff.analyzer.visitors

import br.com.kotsniff.analyzer.utils.TestDslKeywords
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * A PSI tree visitor that identifies Kotest test blocks across multiple Spec styles.
 *
 * Traverses the AST looking for classes that extend a Kotest spec type (directly
 * or through a base class declared in the same file), then extracts test block
 * names and bodies from within those classes.
 *
 * Supported patterns:
 * - **StringSpec**: `"test name" { body }` (string-invoke pattern)
 * - **FunSpec**: `test("name") { body }`, `context("name") { body }`
 * - **BehaviorSpec**: `given/when/then("name") { body }`
 * - **DescribeSpec**: `describe/context/it("name") { body }`
 * - **FeatureSpec**: `feature/scenario("name") { body }`
 * - **ShouldSpec**: `should("name") { body }`
 * - **ExpectSpec**: `expect("name") { body }`
 * - **FreeSpec**: `"name" - { body }`
 * - **WordSpec**: `"name" should { body }`, `"name" When { body }`
 * - **AnnotationSpec**: Methods annotated with `@Test`
 */
class KotestBlockExtractor : KtTreeVisitorVoid() {
    private val _foundTests = mutableListOf<KotestBlock>()

    /** The list of test blocks discovered during traversal. */
    val foundTests: List<KotestBlock> get() = _foundTests.toList()

    private var specDepth = 0

    /** Simple names of classes in the current file that are Kotest specs. */
    private val localSpecNames = mutableSetOf<String>()

    override fun visitKtFile(file: KtFile) {
        localSpecNames.addAll(resolveSpecNames(file))
        super.visitKtFile(file)
    }

    /**
     * Resolves which classes of a file are specs, including classes that extend
     * another spec declared in the same file (transitive inheritance).
     */
    private fun resolveSpecNames(file: KtFile): Set<String> {
        val classes = file.collectDescendantsOfType<KtClassOrObject>()
        val supersByClass =
            classes
                .mapNotNull { klass ->
                    val name = klass.name ?: return@mapNotNull null
                    name to
                        klass.superTypeListEntries.mapNotNull { entry ->
                            entry.typeReference
                                ?.text
                                ?.substringBefore("<")
                                ?.substringAfterLast('.')
                                ?.trim()
                        }
                }.toMap()

        val specNames = mutableSetOf<String>()
        var changed = true
        while (changed) {
            changed = false
            for ((name, supers) in supersByClass) {
                if (name in specNames) continue
                if (supers.any { TestDslKeywords.looksLikeSpecName(it) || it in specNames }) {
                    specNames.add(name)
                    changed = true
                }
            }
        }
        return specNames
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val isSpec = classOrObject.name in localSpecNames

        if (isSpec) {
            specDepth++
            super.visitClassOrObject(classOrObject)
            specDepth--
        } else {
            // Calls inside a non-spec class (even if nested in a spec) are not tests.
            val previous = specDepth
            specDepth = 0
            super.visitClassOrObject(classOrObject)
            specDepth = previous
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        if (specDepth > 0) {
            tryExtractTestBlock(expression)
        }
        super.visitCallExpression(expression)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (specDepth > 0) {
            tryExtractFromBinaryExpression(expression)
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (specDepth > 0) {
            tryExtractFromNamedFunction(function)
        }
        super.visitNamedFunction(function)
    }

    private fun tryExtractFromNamedFunction(function: KtNamedFunction) {
        val hasTestAnnotation = function.annotationEntries.any { it.shortName?.asString() == "Test" }
        if (hasTestAnnotation) {
            val name = function.name ?: return
            val body = function.bodyExpression ?: return
            _foundTests.add(KotestBlock(name, body, function, isTerminal = true))
        }
    }

    private fun tryExtractFromBinaryExpression(expression: KtBinaryExpression) {
        val operation = expression.operationReference.text
        if (operation in TestDslKeywords.ALL_DSL_NAMES) {
            val left = expression.left?.text?.removeSurrounding("\"") ?: return
            val right = expression.right ?: return

            // Kotest DSL blocks always have a lambda on the right side
            if (right !is KtLambdaExpression) return

            // In FreeSpec and WordSpec, the right side is the lambda containing nested tests
            // e.g. "container" - { ... }
            val isTerminal = operation in TestDslKeywords.ALL_TERMINALS
            _foundTests.add(KotestBlock(left, right, expression, isTerminal))
        }
    }

    private fun tryExtractTestBlock(expression: KtCallExpression) {
        val callee = expression.calleeExpression ?: return

        // The test body is always the trailing lambda: test("name") { body }
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return

        when (callee) {
            // StringSpec pattern: "test name" { body }
            is KtStringTemplateExpression -> {
                val name = callee.entries.joinToString("") { it.text }
                _foundTests.add(KotestBlock(name, lambda, expression, isTerminal = true))
            }

            // Named DSL functions: test("name") { }, given("name") { }, etc.
            is KtNameReferenceExpression -> {
                val funcName = callee.getReferencedName()
                if (funcName in TestDslKeywords.ALL_DSL_NAMES) {
                    val name = extractFirstStringArgument(expression) ?: funcName
                    val isTerminal = funcName in TestDslKeywords.ALL_TERMINALS
                    _foundTests.add(KotestBlock(name, lambda, expression, isTerminal))
                }
            }
        }
    }

    private fun extractFirstStringArgument(expression: KtCallExpression): String? {
        val firstArg = expression.valueArguments.firstOrNull()?.getArgumentExpression()
        return (firstArg as? KtStringTemplateExpression)?.entries?.joinToString("") { it.text }
    }
}
