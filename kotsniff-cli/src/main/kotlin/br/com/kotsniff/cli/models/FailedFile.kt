package br.com.kotsniff.cli.models

import kotlinx.serialization.Serializable

/**
 * A file that could not be analysed.
 *
 * @property filePath Path relative to the scanned root.
 * @property reason Human-readable cause (syntax error, I/O error, ...).
 */
@Serializable
data class FailedFile(
    val filePath: String,
    val reason: String,
)
