package br.com.kotsniff.cli.runner

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.cli.models.FailedFile
import br.com.kotsniff.cli.models.ScanSummary
import br.com.kotsniff.cli.utils.GlobMatcher
import br.com.kotsniff.core.models.Diagnostic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Discovers Kotlin files under a root directory and analyses them concurrently.
 *
 * Files matching any exclusion glob are skipped. Files that cannot be read or
 * that contain syntax errors are reported as failures instead of being silently
 * ignored or producing unreliable diagnostics.
 */
class KotSniffRunner(
    private val engine: KotSniffEngine,
) {
    /**
     * Scans a directory for Kotlin files and analyzes them concurrently.
     *
     * @param rootDir Directory to scan.
     * @param excludePatterns Additional glob patterns to exclude.
     * @param onProgress Optional callback invoked after each file with
     *   `(processed, total)`. Must be safe to call from worker threads.
     * @return A [ScanSummary] containing the results and any failed files.
     */
    fun scanDirectory(
        rootDir: File,
        excludePatterns: List<String> = emptyList(),
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): ScanSummary {
        val matcher = GlobMatcher(DEFAULT_EXCLUDES + excludePatterns)

        val candidateFiles =
            rootDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()

        val filesToAnalyze =
            candidateFiles.filter { file ->
                val relative = file.relativeTo(rootDir).invariantSeparatorsPath
                !matcher.matches(relative)
            }
        val ignoredFiles = candidateFiles.size - filesToAnalyze.size
        val total = filesToAnalyze.size
        val processed = AtomicInteger(0)

        val outcomes =
            runBlocking(Dispatchers.Default) {
                filesToAnalyze
                    .map { file ->
                        async {
                            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                            val outcome =
                                try {
                                    val text = file.readText()
                                    val loc = if (text.isEmpty()) 0 else text.lines().size
                                    val analysis = engine.analyzeDetailed(text, relativePath)
                                    val failure =
                                        if (analysis.hasSyntaxErrors) {
                                            FailedFile(relativePath, "syntax error")
                                        } else {
                                            null
                                        }
                                    FileOutcome(analysis.diagnostics, loc, failure)
                                } catch (e: Exception) {
                                    FileOutcome(emptyList(), 0, FailedFile(relativePath, "I/O error: ${e.message}"))
                                }
                            onProgress?.invoke(processed.incrementAndGet(), total)
                            outcome
                        }
                    }.awaitAll()
            }

        return ScanSummary(
            totalFilesScanned = total,
            totalLinesOfCode = outcomes.sumOf { it.linesOfCode },
            ignoredFiles = ignoredFiles,
            diagnostics = outcomes.flatMap { it.diagnostics },
            failedFiles = outcomes.mapNotNull { it.failure },
        )
    }

    private data class FileOutcome(
        val diagnostics: List<Diagnostic>,
        val linesOfCode: Int,
        val failure: FailedFile?,
    )

    companion object {
        /** Default directories that are never useful to analyse. */
        val DEFAULT_EXCLUDES = listOf("**/build/**", "**/.gradle/**", "**/.git/**", "**/node_modules/**")
    }
}
