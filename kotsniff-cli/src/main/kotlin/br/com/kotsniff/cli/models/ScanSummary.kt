package br.com.kotsniff.cli.models

import br.com.kotsniff.core.models.DetectionResult

data class ScanSummary(
    val totalFilesScanned: Int,
    val smells: List<DetectionResult>,
    val failedFiles: List<String>,
)
