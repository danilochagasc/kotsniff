package br.com.kotsniff.analyzer.utils

/**
 * MockK DSL vocabulary.
 *
 * Separates *verifications* (which behave like assertions) from *stubbing*
 * (which does not), so detectors can reason about mock interactions instead of
 * relying on accidental name prefixes.
 */
object MockkDslKeywords {
    /** Verification primitives that assert an interaction happened. */
    val VERIFICATION_FUNCTIONS =
        setOf(
            "verify",
            "coVerify",
            "verifyOrder",
            "coVerifyOrder",
            "verifySequence",
            "coVerifySequence",
            "verifyAll",
            "coVerifyAll",
            "confirmVerified",
            "checkUnnecessaryStub",
        )

    /** Stubbing primitives (they configure behaviour, they do not assert it). */
    val STUBBING_FUNCTIONS =
        setOf(
            "every",
            "coEvery",
            "everyAnswer",
            "coEveryAnswer",
            "mMockk",
            "mockk",
            "spyk",
            "mockkStatic",
            "mockkObject",
        )
}
