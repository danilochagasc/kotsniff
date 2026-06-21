package br.com.kotsniff.analyzer.utils

object KotestDslKeywords {
    // Containers
    private val DESCRIBE_SPEC_CONTAINERS = setOf("describe", "xdescribe", "context", "xcontext")
    private val BEHAVIOR_SPEC_CONTAINERS = setOf("given", "Given", "xgiven", "when", "When", "xwhen", "and", "And", "xand")
    private val FEATURE_SPEC_CONTAINERS = setOf("feature", "xfeature")
    private val FREE_SPEC_CONTAINERS = setOf("-")
    private val WORD_SPEC_CONTAINERS = setOf("should", "When")
    private val SHARED_CONTAINERS = setOf("context", "xcontext")

    // Terminals
    private val DESCRIBE_SPEC_TERMINALS = setOf("it", "xit")
    private val BEHAVIOR_SPEC_TERMINALS = setOf("then", "Then", "xthen")
    private val FEATURE_SPEC_TERMINALS = setOf("scenario", "xscenario")
    private val FUN_SPEC_TERMINALS = setOf("test", "xtest")
    private val SHOULD_SPEC_TERMINALS = setOf("should", "xshould")
    private val EXPECT_SPEC_TERMINALS = setOf("expect", "xexpect")
    private val ANNOTATION_SPEC_TERMINALS = setOf("Test")

    val ALL_CONTAINERS =
        DESCRIBE_SPEC_CONTAINERS +
            BEHAVIOR_SPEC_CONTAINERS +
            FEATURE_SPEC_CONTAINERS +
            FREE_SPEC_CONTAINERS +
            WORD_SPEC_CONTAINERS +
            SHARED_CONTAINERS

    val ALL_TERMINALS =
        DESCRIBE_SPEC_TERMINALS +
            BEHAVIOR_SPEC_TERMINALS +
            FEATURE_SPEC_TERMINALS +
            FUN_SPEC_TERMINALS +
            SHOULD_SPEC_TERMINALS +
            EXPECT_SPEC_TERMINALS +
            ANNOTATION_SPEC_TERMINALS

    val ALL_DSL_NAMES = ALL_CONTAINERS + ALL_TERMINALS
}
