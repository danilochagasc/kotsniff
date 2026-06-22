package br.com.kotsniff.cli.models

import br.com.kotsniff.core.models.DetectionResult
import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val projectName: String,
    val totalFilesScanned: Int,
    val totalLinesOfCode: Int,
    val timeTakenMs: Long,
    val smellsFound: Int,
    val smellsByType: Map<String, Int>,
    val smells: List<DetectionResult>,
    val failedFiles: List<String> = emptyList(),
)
