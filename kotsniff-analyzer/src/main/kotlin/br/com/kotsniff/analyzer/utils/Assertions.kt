package br.com.kotsniff.analyzer.utils

import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Centralised recognition of **assertions** and **mock verifications**.
 *
 * Using a curated vocabulary instead of a naive prefix match avoids false
 * negatives/positives such as a domain method named `expectStatus(...)` or
 * `verifyInput(...)` being mistaken for an assertion.
 *
 * Recognised shapes:
 * - **Infix matchers**: `actual shouldBe expected` (a `KtBinaryExpression` whose
 *   operation starts with `should`).
 * - **Dot-qualified matchers**: `actual.shouldBe(expected)` (selector name starts
 *   with `should`).
 * - **Known assertion/verification calls**: `assert(...)`, `expect(...)`,
 *   `assertFailsWith(...)`, MockK `verify {}`, `coVerify {}`, `verifyOrder {}`,
 *   `verifySequence {}`, `confirmVerified(...)`, etc.
 */
object Assertions {
    /** Kotlin/Kotest assertion helpers that are unambiguous top-level calls. */
    private val KNOWN_ASSERTION_FUNCTIONS =
        setOf(
            "assert",
            "assertTrue",
            "assertFalse",
            "assertNull",
            "assertNotNull",
            "assertEquals",
            "assertNotEquals",
            "assertSame",
            "assertNotSame",
            "assertContentEquals",
            "assertFails",
            "assertFailsWith",
            "assertSoftly",
            "expect",
            "expectThat",
            "fail",
            "error",
        )

    private const val MATCHER_PREFIX = "should"

    /** Matcher function names (Kotest matchers all start with `should`). */
    fun isMatcherName(name: String): Boolean = name.startsWith(MATCHER_PREFIX)

    /** Known assertion helpers (excluding matchers). */
    fun isKnownAssertionFunction(name: String): Boolean = name in KNOWN_ASSERTION_FUNCTIONS

    /** MockK verification functions. */
    fun isMockVerification(name: String): Boolean = name in MockkDslKeywords.VERIFICATION_FUNCTIONS

    /** MockK stubbing functions (they configure behaviour; they do not assert it). */
    fun isStubbingFunction(name: String): Boolean = name in MockkDslKeywords.STUBBING_FUNCTIONS

    /** Any callee name that counts as an assertion or verification. */
    fun isAssertionFunction(name: String): Boolean = isKnownAssertionFunction(name) || isMockVerification(name) || isMatcherName(name)

    /** Whether a call expression is an assertion/verification call. */
    fun KtCallExpression.isAssertionCall(): Boolean {
        val name = calleeName() ?: return false
        return isAssertionFunction(name)
    }

    /** Whether a binary expression is an infix assertion (e.g. `a shouldBe b`). */
    fun KtBinaryExpression.isAssertionOperation(): Boolean {
        val operation = operationReference.text
        return isMatcherName(operation) || isKnownAssertionFunction(operation)
    }

    /**
     * Extracts the invoked function name of a call expression, handling both
     * plain calls (`assert(...)`) and qualified ones (`list.shouldBe(...)`).
     */
    fun KtCallExpression.calleeName(): String? {
        val callee = calleeExpression ?: return null
        return when (callee) {
            is KtNameReferenceExpression -> callee.getReferencedName()
            else -> callee.text
        }
    }

    /**
     * Collects the **assertion units** inside [root], applying the single,
     * consistent policy shared by every detector:
     *
     * - A recognised assertion/verification call (or infix matcher) is **one**
     *   unit; traversal does **not** descend into it, so nested matchers inside
     *   the same assertion expression are not counted again. This makes
     *   compound helpers such as `assertSoftly { ... }` / `assertAll { ... }`
     *   count as a single logical assertion instead of wrapper + children.
     * - For a dot-qualified matcher (`x.shouldBe(y)`) the unit is the whole
     *   qualified expression, so its text keeps the receiver.
     * - Assertions nested inside ordinary calls/lambdas (e.g.
     *   `list.forEach { it shouldBe 1 }`) are still found, because those calls
     *   are not assertions and traversal continues through them.
     *
     * The returned units are in source order.
     */
    fun collectAssertionUnits(root: KtElement): List<KtExpression> {
        val units = mutableListOf<KtExpression>()
        root.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.isAssertionCall()) {
                        val parent = expression.parent
                        val unit =
                            if (parent is KtDotQualifiedExpression && parent.selectorExpression === expression) {
                                parent
                            } else {
                                expression
                            }
                        units.add(unit)
                        return
                    }
                    super.visitCallExpression(expression)
                }

                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.isAssertionOperation()) {
                        units.add(expression)
                        return
                    }
                    super.visitBinaryExpression(expression)
                }
            },
        )
        return units
    }
}
