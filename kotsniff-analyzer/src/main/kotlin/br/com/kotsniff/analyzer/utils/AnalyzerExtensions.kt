package br.com.kotsniff.analyzer.utils

fun String?.isAssertion(): Boolean {
    if (this == null) return false
    return KotestDslKeywords.ASSERTION_PREFIXES.any { this.startsWith(it) }
}
