package br.com.kotsniff.core.models

import kotlinx.serialization.Serializable

/**
 * Represents a specific range within a source file.
 *
 * All line and column numbers are 1-based. [endColumn] points to the first
 * character *after* the last character of the range, following the same
 * convention as many editors and SARIF.
 *
 * @property filePath The path to the source file.
 * @property line The 1-based starting line number.
 * @property column The 1-based starting column number.
 * @property endLine The 1-based ending line number.
 * @property endColumn The 1-based ending column number (exclusive), defaulting to [column].
 */
@Serializable
data class CodeLocation(
    val filePath: String,
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int = column,
)
