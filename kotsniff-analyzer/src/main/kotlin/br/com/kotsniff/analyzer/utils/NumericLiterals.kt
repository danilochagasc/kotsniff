package br.com.kotsniff.analyzer.utils

/**
 * Helpers for interpreting Kotlin numeric literals.
 *
 * Handles underscores (`1_000`), hexadecimal/binary prefixes (`0xFF`, `0b1010`)
 * and type suffixes (`L`, `f`, `u`), which a naive `toDoubleOrNull()` would miss.
 */
object NumericLiterals {
    /** Literals that are considered self-explanatory and thus not "magic". */
    val DEFAULT_ALLOWED: Set<String> = setOf("0", "1", "100")

    /** Removes underscores and trailing type suffixes, keeping the radix prefix. */
    fun normalize(text: String): String {
        val withoutUnderscores = text.replace("_", "")
        return withoutUnderscores
            .removeSuffix("L")
            .removeSuffix("l")
            .removeSuffix("F")
            .removeSuffix("f")
            .removeSuffix("U")
            .removeSuffix("u")
    }

    /** Whether [text] is a valid Kotlin numeric literal. */
    fun isNumericLiteral(text: String): Boolean {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return false
        return when {
            normalized.startsWith("0x", ignoreCase = true) -> {
                normalized.substring(2).isNotEmpty() && normalized.substring(2).toLongOrNull(16) != null
            }

            normalized.startsWith("0b", ignoreCase = true) -> {
                normalized.substring(2).isNotEmpty() && normalized.substring(2).toLongOrNull(2) != null
            }

            else -> {
                normalized.toDoubleOrNull() != null
            }
        }
    }
}
