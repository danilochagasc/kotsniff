package br.com.kotsniff.cli.runner

import br.com.kotsniff.analyzer.KotSniffEngine
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
     * @return A Pair containing the total number of files scanned and a flat list of detection results.
     */
    fun scanDirectory(rootDir: File): Pair<Int, List<DetectionResult>> {
        val ktFiles = rootDir.walkTopDown().filter { it.extension == "kt" }.toList()

        val results =
            runBlocking(Dispatchers.Default) {
                val deferredResults =
                    ktFiles.map { file ->
                        async {
                            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                            try {
                                engine.analyze(file.readText(), relativePath)
                            } catch (e: Exception) {
                                System.err.println("Warning: Failed to parse file ${relativePath} - ${e.message}")
                                emptyList<DetectionResult>()
                            }
                        }
                    }
                deferredResults.awaitAll().flatten()
            }

        return Pair(ktFiles.size, results)
    }
}
