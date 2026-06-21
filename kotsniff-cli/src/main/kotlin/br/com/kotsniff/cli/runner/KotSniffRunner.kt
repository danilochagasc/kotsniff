package br.com.kotsniff.cli.runner

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.cli.models.ScanSummary
import br.com.kotsniff.core.models.DetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

class KotSniffRunner(
    private val engine: KotSniffEngine,
) {
    /**
     * Scans a directory for Kotlin files and analyzes them concurrently.
     * @return A [ScanSummary] containing the results and any failed files.
     */
    fun scanDirectory(rootDir: File): ScanSummary {
        val ktFiles = rootDir.walkTopDown().filter { it.extension == "kt" }.toList()

        val resultsAndFailures =
            runBlocking(Dispatchers.Default) {
                val deferredResults =
                    ktFiles.map { file ->
                        async {
                            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                            try {
                                val fileSmells = engine.analyze(file.readText(), relativePath)
                                Pair(fileSmells, null)
                            } catch (e: Exception) {
                                Pair(emptyList<DetectionResult>(), relativePath)
                            }
                        }
                    }
                deferredResults.awaitAll()
            }

        val results = resultsAndFailures.flatMap { it.first }
        val failedFiles = resultsAndFailures.mapNotNull { it.second }

        return ScanSummary(
            totalFilesScanned = ktFiles.size,
            smells = results,
            failedFiles = failedFiles,
        )
    }
}
