package br.com.kotsniff.analyzer.utils

fun String?.isAssertion(): Boolean {
    if (this == null) return false
    return TestDslKeywords.ASSERTION_REGEX.containsMatchIn(this)
}
