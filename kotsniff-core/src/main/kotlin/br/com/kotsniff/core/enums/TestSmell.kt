package br.com.kotsniff.core.enums

import kotlinx.serialization.Serializable

/**
 * Enumerates the types of test smells that KotSniff can detect in Kotest test suites.
 *
 * @property description A human-readable explanation of the smell.
 */
@Serializable
enum class TestSmell(
    val description: String,
) {
    IGNORED_TEST("Tests that are disabled or skipped."),
    SLEEPY_TEST("Tests that use explicit thread sleeps, e.g., Thread.sleep."),
    CONDITIONAL_TEST_LOGIC("Tests containing if/else or when blocks, which indicates the test might have multiple flows."),
    EXCEPTION_HANDLING("Test methods should not use try/catch blocks. The test framework should handle exceptions."),
    UNKNOWN_TEST("A test block that executes code but contains NO assertions."),
    DUPLICATE_ASSERT("Testing the exact same condition multiple times in the same test block."),
    MAGIC_NUMBER_TEST("Using arbitrary numeric literals in assertions without explaining their meaning via constants."),
    ASSERTION_ROULETTE("Having too many assertions in a single test block, making it hard to identify the failure reason."),
    ;

    /**
     * A pretty-printed name for the smell (e.g. IGNORED_TEST -> "Ignored Test").
     */
    val displayName: String
        get() = name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
