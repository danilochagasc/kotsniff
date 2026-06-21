package br.com.kotsniff.analyzer.visitors

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression

/**
 * Represents a Kotest test block found during AST traversal.
 *
 * @property name The display name of the test (e.g., the string passed to `test("...")` or `"..." { }`).
 * @property body The lambda expression containing the test body.
 */
data class KotestBlock(
    val name: String,
    val body: KtElement,
    val originalExpression: KtElement,
    val isTerminal: Boolean,
)
