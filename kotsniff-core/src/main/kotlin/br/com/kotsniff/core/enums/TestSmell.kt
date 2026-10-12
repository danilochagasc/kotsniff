package br.com.kotsniff.core.enums

import kotlinx.serialization.Serializable

/**
 * Enumerates the types of test smells that KotSniff can detect, and doubles as
 * the **stable rule identifier** used across analysis, configuration and every
 * report format.
 *
 * @property ruleName A human-readable name for the smell (e.g. "Ignored Test").
 * @property description A human-readable explanation of the smell.
 * @property defaultSeverity The severity assigned when no override is configured.
 */
@Serializable
enum class TestSmell(
    val ruleName: String,
    val description: String,
    val defaultSeverity: Severity,
) {
    IGNORED_TEST(
        "Ignored Test",
        "Tests that are disabled or skipped.",
        Severity.MEDIUM,
    ),
    SLEEPY_TEST(
        "Sleepy Test",
        "Tests that block or pause execution with sleeps, e.g., Thread.sleep or delay.",
        Severity.MEDIUM,
    ),
    CONDITIONAL_TEST_LOGIC(
        "Conditional Test Logic",
        "Tests containing control-flow conditionals, which indicates the test might cover multiple flows.",
        Severity.MEDIUM,
    ),
    EXCEPTION_HANDLING(
        "Exception Handling",
        "Test methods should not use try/catch blocks. The test framework should handle exceptions.",
        Severity.MEDIUM,
    ),
    UNKNOWN_TEST(
        "Unknown Test",
        "A test block that executes code but contains no assertions or mock verifications.",
        Severity.HIGH,
    ),
    DUPLICATE_ASSERT(
        "Duplicate Assert",
        "Testing the exact same condition multiple times in the same test block.",
        Severity.LOW,
    ),
    MAGIC_NUMBER_TEST(
        "Magic Number Test",
        "Using arbitrary numeric literals in assertions without explaining their meaning via constants.",
        Severity.LOW,
    ),
    ASSERTION_ROULETTE(
        "Assertion Roulette",
        "Having too many assertions in a single test block, making it hard to identify the failure reason.",
        Severity.MEDIUM,
    ),
    ;

    /**
     * Resolves a [TestSmell] from a rule name or enum name, case-insensitively.
     */
    companion object {
        fun fromId(id: String): TestSmell? {
            val normalized =
                id
                    .trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .uppercase()
            return entries.firstOrNull { it.name == normalized || it.ruleName.equals(id.trim(), ignoreCase = true) }
        }
    }
}
