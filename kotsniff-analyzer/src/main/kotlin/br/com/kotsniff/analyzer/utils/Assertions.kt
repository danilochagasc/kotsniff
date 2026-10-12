package br.com.kotsniff.analyzer.utils

import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

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
}
