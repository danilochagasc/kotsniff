package br.com.kotsniff.core.models

import br.com.kotsniff.core.enums.TestSmell
import kotlinx.serialization.Serializable

/**
 * Represents a single test smell detection found during analysis.
 *
 * @property smellType The category of test smell detected.
 * @property location The exact source code location where the smell was found.
 * @property codeSnippet A snippet of the code that triggered the detection.
 */
@Serializable
data class DetectionResult(
    val smellType: TestSmell,
    val location: CodeLocation,
    val codeSnippet: String,
)
