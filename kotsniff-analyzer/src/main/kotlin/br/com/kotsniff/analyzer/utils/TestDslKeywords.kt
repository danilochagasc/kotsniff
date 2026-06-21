package br.com.kotsniff.analyzer.utils

object TestDslKeywords {
    val ALL_CONTAINERS = KotestDslKeywords.ALL_CONTAINERS
    val ALL_TERMINALS = KotestDslKeywords.ALL_TERMINALS
    val ALL_DSL_NAMES = KotestDslKeywords.ALL_DSL_NAMES

    val ASSERTION_PREFIXES = KotestDslKeywords.ASSERTION_PREFIXES + MockkDslKeywords.VERIFICATION_PREFIXES
    val ASSERTION_REGEX = Regex("^(${ASSERTION_PREFIXES.joinToString("|")}).*")
}
