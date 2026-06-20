package br.com.kotsniff.cli.models

import br.com.kotsniff.core.models.DetectionResult
import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val totalFilesScanned: Int,
    val timeTakenMs: Long,
    val smellsFound: Int,
    val smells: List<DetectionResult>,
)
