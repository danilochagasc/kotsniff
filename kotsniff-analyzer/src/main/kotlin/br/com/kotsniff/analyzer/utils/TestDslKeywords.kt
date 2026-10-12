package br.com.kotsniff.analyzer.utils

/**
 * Aggregates the DSL keywords used to identify Kotest test blocks.
 *
 * Kept as a small façade so visitors depend on a single entry point.
 */
object TestDslKeywords {
    val ALL_CONTAINERS = KotestDslKeywords.ALL_CONTAINERS
    val ALL_TERMINALS = KotestDslKeywords.ALL_TERMINALS
    val ALL_DSL_NAMES = KotestDslKeywords.ALL_DSL_NAMES
    val BINARY_CONTAINER_OPERATORS = KotestDslKeywords.BINARY_CONTAINER_OPERATORS
    val KNOWN_SPEC_SUPERTYPES = KotestDslKeywords.KNOWN_SPEC_SUPERTYPES

    fun looksLikeSpecName(simpleName: String): Boolean = KotestDslKeywords.looksLikeSpecName(simpleName)
}
