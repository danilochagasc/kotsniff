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
    CONDITIONAL_TEST_LOGIC("Tests containing if/else or when blocks, which indicates the test might have multiple flows.");

    /**
     * A pretty-printed name for the smell (e.g. IGNORED_TEST -> "Ignored Test").
     */
    val displayName: String
        get() = name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
