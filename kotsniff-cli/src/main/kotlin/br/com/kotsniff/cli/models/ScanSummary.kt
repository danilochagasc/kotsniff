package br.com.kotsniff.cli.models

import br.com.kotsniff.core.models.Diagnostic

/**
 * Aggregated result of scanning a directory.
 *
 * @property totalFilesScanned Number of Kotlin files analysed.
 * @property totalLinesOfCode Total lines across all analysed files.
 * @property ignoredFiles Number of Kotlin files skipped due to exclusion rules.
 * @property diagnostics All detected smells.
 * @property failedFiles Files that could not be analysed.
 */
data class ScanSummary(
    val totalFilesScanned: Int,
    val totalLinesOfCode: Int,
    val ignoredFiles: Int,
    val diagnostics: List<Diagnostic>,
    val failedFiles: List<FailedFile>,
)
