package br.com.kotsniff.cli.ui

import br.com.kotsniff.cli.models.FailedFile
import br.com.kotsniff.cli.utils.formatCodeSnippet
import br.com.kotsniff.core.models.Diagnostic

/**
 * Encapsulates all terminal output with semantic, optionally colored methods.
 *
 * Isolates presentation concerns from the command orchestration logic, making it
 * easy to swap for a silent or test-friendly printer.
 */
class TerminalPrinter(
    private val echo: (String, Boolean) -> Unit,
    private val rawEcho: (String) -> Unit = {},
    private val useColor: Boolean = true,
    private val quiet: Boolean = false,
) {
    private var progressShown = false

    fun printStart() {
        if (quiet) return
        echo("${paint(BLUE, "[*]")} Starting KotSniff Analysis...", false)
    }

    /** Renders a single self-overwriting progress line (used for long analyses). */
    fun printProgress(
        processed: Int,
        total: Int,
    ) {
        if (quiet) return
        progressShown = true
        rawEcho("${paint(CYAN, "[...]")} Analysing $processed/$total files...")
    }

    /** Clears the progress line, if one was shown. */
    fun finishProgress() {
        if (quiet || !progressShown) return
        progressShown = false
        rawEcho("\r${" ".repeat(60)}\r")
    }

    fun printError(message: String) {
        echo("${paint(RED, "[ERROR]")} $message", true)
    }

    fun printWarning(message: String) {
        if (quiet) return
        echo("${paint(YELLOW, "[WARN]")} $message", false)
    }

    fun printScanSummary(
        totalFiles: Int,
        ignoredFiles: Int,
        failedFiles: Int,
        timeTakenMs: Long,
    ) {
        if (quiet) return
        val ignored = if (ignoredFiles > 0) ", $ignoredFiles ignored" else ""
        echo("${paint(GREEN, "[OK]")} Scanned $totalFiles Kotlin file(s)$ignored in ${timeTakenMs}ms", false)
        if (failedFiles > 0) {
            echo("${paint(YELLOW, "[WARN]")} $failedFiles file(s) could not be analysed", false)
        }
    }

    fun printResults(diagnostics: List<Diagnostic>) {
        if (quiet) return

        if (diagnostics.isEmpty()) {
            echo("${paint(GREEN, "[SUCCESS]")} No test smells found!", false)
            return
        }

        val bySeverity = diagnostics.groupingBy { it.severity.label }.eachCount()
        val summary = bySeverity.entries.joinToString(", ") { "${it.value} ${it.key}" }
        echo("${paint(YELLOW, "[WARN]")} Found ${diagnostics.size} test smell(s): $summary", false)

        diagnostics
            .sortedWith(compareBy({ it.location.filePath }, { it.location.line }))
            .groupBy { it.location.filePath }
            .forEach { (file, smells) ->
                echo("\n${paint(CYAN, "->")} $file", false)
                smells.forEach { smell ->
                    echo(
                        "  - ${paint(RED, "[${smell.ruleName}]")} (${smell.severity.label}) line ${smell.location.line}: ${smell.message}",
                        false,
                    )
                    echo(smell.codeSnippet.formatCodeSnippet(), false)
                    smell.suggestion?.let { echo("    -> $it", false) }
                }
            }
    }

    fun printReportLocation(path: String) {
        if (quiet) return
        echo("\n${paint(GREEN, "[INFO]")} Report generated at: $path", false)
    }

    fun printFailedFiles(failedFiles: List<FailedFile>) {
        if (failedFiles.isEmpty() || quiet) return

        echo("\n${paint(YELLOW, "[WARN]")} ${failedFiles.size} file(s) could not be analysed:", false)
        failedFiles.forEach { file ->
            echo("  - ${file.filePath} (${file.reason})", false)
        }
    }

    private fun paint(
        code: String,
        text: String,
    ): String = if (useColor) "$code$text$RESET" else text

    companion object {
        private const val RESET = "\u001B[0m"
        private const val RED = "\u001B[31m"
        private const val GREEN = "\u001B[32m"
        private const val YELLOW = "\u001B[33m"
        private const val BLUE = "\u001B[34m"
        private const val CYAN = "\u001B[36m"
    }
}
