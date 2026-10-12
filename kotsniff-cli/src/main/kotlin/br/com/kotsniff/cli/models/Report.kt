package br.com.kotsniff.cli.models

import br.com.kotsniff.core.models.Diagnostic
import kotlinx.serialization.Serializable

/**
 * Serializable analysis report shared by every report generator.
 *
 * @property schemaVersion Version of this JSON schema; bump on breaking changes.
 * @property toolVersion Version of KotSniff that produced the report.
 * @property severityCounts Number of findings per severity label.
 * @property smellsByType Number of findings per rule name.
 */
@Serializable
data class Report(
    val schemaVersion: Int = SCHEMA_VERSION,
    val toolVersion: String,
    val projectName: String,
    val totalFilesScanned: Int,
    val totalLinesOfCode: Int,
    val ignoredFiles: Int = 0,
    val timeTakenMs: Long,
    val smellsFound: Int,
    val severityCounts: Map<String, Int>,
    val smellsByType: Map<String, Int>,
    val smells: List<Diagnostic>,
    val failedFiles: List<FailedFile> = emptyList(),
) {
    companion object {
        /** Current report schema version. */
        const val SCHEMA_VERSION = 2
    }
}
