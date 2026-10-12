package br.com.kotsniff.cli.config

import kotlinx.serialization.Serializable

/**
 * Optional on-disk configuration, read from `kotsniff.json` in the scanned root
 * (or from `--config <path>`). Every field is optional and merged with the CLI
 * arguments (command-line values win).
 */
@Serializable
data class FileConfig(
    val exclude: List<String> = emptyList(),
    val disabledRules: List<String> = emptyList(),
    val maxAssertions: Int? = null,
    val reportDelay: Boolean? = null,
    val allowedNumbers: List<String>? = null,
    val severities: Map<String, String> = emptyMap(),
    val reportMaxLines: Int? = null,
)
