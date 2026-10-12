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

    /**
     * DSL forms written as **infix binary expressions** (`"name" - { }`,
     * `"name" should { }`, `"name" When { }`). Unlike the call forms, these are
     * always *containers*: WordSpec/FreeSpec express leaf tests via a plain
     * string-invoke (`"name" { }`), never via the infix operator. Treating them
     * as terminals would leak nested tests' assertions into the container.
     */
    val BINARY_CONTAINER_OPERATORS = FREE_SPEC_CONTAINERS + WORD_SPEC_CONTAINERS

    /**
     * Canonical Kotest base spec types. Used to decide whether a class is a test
     * spec. Kotest specs may also be defined indirectly (a local base class that
     * extends one of these), which [KotestBlockExtractor] resolves within a file.
     */
    val KNOWN_SPEC_SUPERTYPES =
        setOf(
            "StringSpec",
            "FunSpec",
            "BehaviorSpec",
            "DescribeSpec",
            "ShouldSpec",
            "ExpectSpec",
            "FreeSpec",
            "WordSpec",
            "FeatureSpec",
            "AnnotationSpec",
        )

    /** Heuristic: a supertype whose simple name ends with `Spec`. */
    fun looksLikeSpecName(simpleName: String): Boolean = simpleName in KNOWN_SPEC_SUPERTYPES || simpleName.endsWith("Spec")
}
