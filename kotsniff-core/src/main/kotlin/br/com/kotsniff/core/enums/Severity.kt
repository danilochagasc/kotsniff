package br.com.kotsniff.core.enums

import kotlinx.serialization.Serializable

/**
 * Severity level assigned to a detected test smell.
 *
 * The severity is used by reports and by the CLI to prioritise diagnostics.
 * It is intentionally coarse (four levels) to avoid false precision.
 */
@Serializable
enum class Severity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    ;

    /** Lowercase human-readable label (e.g. `HIGH` -> `high`). */
    val label: String
        get() = name.lowercase()

    companion object {
        /** Parses a severity from its [name] or [label], case-insensitively; `null` if unknown. */
        fun fromId(id: String): Severity? {
            val normalized = id.trim().lowercase()
            return entries.firstOrNull { it.name.lowercase() == normalized }
        }
    }
}
