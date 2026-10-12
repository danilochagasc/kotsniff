package br.com.kotsniff.analyzer.visitors

import org.jetbrains.kotlin.psi.KtElement

/**
 * Represents a Kotest test block found during AST traversal.
 *
 * @property name The display name of the test (e.g., the string passed to `test("...")` or `"..." { }`).
 * @property body The lambda expression containing the test body.
 * @property originalExpression The original PSI element used for locating the block.
 * @property isTerminal Whether this block is a leaf test (as opposed to a container).
 */
data class KotestBlock(
    val name: String,
    val body: KtElement,
    val originalExpression: KtElement,
    val isTerminal: Boolean,
)
