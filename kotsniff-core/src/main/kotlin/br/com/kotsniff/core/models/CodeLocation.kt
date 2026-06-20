package br.com.kotsniff.core.models

import kotlinx.serialization.Serializable

/**
 * Represents a specific location within a source file.
 *
 * @property filePath The path to the source file.
 * @property line The 1-based line number.
 * @property column The 1-based column number.
 */
@Serializable
data class CodeLocation(
    val filePath: String,
    val line: Int,
    val column: Int,
)
