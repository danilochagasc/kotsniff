package br.com.kotsniff.cli.ui

import br.com.kotsniff.cli.utils.formatCodeSnippet
import br.com.kotsniff.core.models.DetectionResult

/**
 * Encapsulates all terminal output with semantic ANSI-colored methods.
 *
 * Isolates presentation concerns from the command orchestration logic,
 * making it easy to swap for a silent or test-friendly printer.
 */
class TerminalPrinter(
    private val echo: (String, Boolean) -> Unit,
) {
    fun printStart() {
        echo("$BLUE🔍 Starting KotSniff Analysis...$RESET", false)
    }

    fun printError(message: String) {
        echo("$RED❌ Error: $message$RESET", true)
    }

    fun printScanSummary(
        totalFiles: Int,
        timeTakenMs: Long,
    ) {
        echo("$GREEN✅ Scanned $totalFiles Kotlin files in ${timeTakenMs}ms$RESET", false)
    }

    fun printResults(results: List<DetectionResult>) {
        if (results.isEmpty()) {
            echo("$GREEN🎉 Awesome! No test smells found!$RESET", false)
            return
        }

        echo("$YELLOW⚠️  Found ${results.size} test smells:$RESET", false)

        results
            .groupBy { it.location.filePath }
            .forEach { (file, smells) ->
                echo("\n$CYAN📁 $file$RESET", false)
                smells.forEach { smell ->
                    echo("  - $RED[${smell.smellType}]$RESET line ${smell.location.line}:\n${smell.codeSnippet.formatCodeSnippet()}", false)
                }
            }
    }

    fun printReportLocation(path: String) {
        echo("\n$GREEN📄 Report generated at: $path$RESET", false)
    }

    companion object {
        private const val RESET = "\u001B[0m"
        private const val RED = "\u001B[31m"
        private const val GREEN = "\u001B[32m"
        private const val YELLOW = "\u001B[33m"
        private const val BLUE = "\u001B[34m"
        private const val CYAN = "\u001B[36m"
    }
}
