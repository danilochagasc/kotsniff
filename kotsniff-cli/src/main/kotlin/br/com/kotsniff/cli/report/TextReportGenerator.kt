package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.cli.utils.formatCodeSnippet
import java.io.File

/**
 * Generates a human-readable plain-text report.
 */
class TextReportGenerator : ReportGenerator {
    override fun generate(
        report: Report,
        outputFile: File,
        targetDir: File,
    ) {
        val content =
            buildString {
                appendLine("KotSniff Report (v${report.toolVersion})")
                appendLine("Project: ${report.projectName}")
                appendLine("═══════════════════════════════════════")
                appendLine("Files scanned: ${report.totalFilesScanned} (${report.ignoredFiles} ignored)")
                appendLine("Lines of code: ${report.totalLinesOfCode}")
                appendLine("Time taken: ${report.timeTakenMs}ms")
                appendLine("Smells found: ${report.smellsFound}")
                if (report.severityCounts.isNotEmpty()) {
                    appendLine()
                    appendLine("By severity:")
                    report.severityCounts.forEach { (severity, count) -> appendLine("  - $count $severity") }
                }
                if (report.smellsByType.isNotEmpty()) {
                    appendLine()
                    appendLine("Breakdown by rule:")
                    report.smellsByType.forEach { (type, count) -> appendLine("  - $count $type") }
                }
                appendLine("═══════════════════════════════════════")

                if (report.smells.isEmpty()) {
                    appendLine("\nNo test smells found!")
                } else {
                    report.smells
                        .groupBy { it.location.filePath }
                        .forEach { (file, smells) ->
                            appendLine("\nFile: $file")
                            smells.forEach { smell ->
                                appendLine(
                                    "  - [${smell.ruleName}] (${smell.severity.label}) line ${smell.location.line}: ${smell.message}",
                                )
                                appendLine(smell.codeSnippet.formatCodeSnippet())
                                smell.suggestion?.let { appendLine("    -> $it") }
                            }
                        }
                }

                if (report.failedFiles.isNotEmpty()) {
                    appendLine("\nFailed to analyse:")
                    report.failedFiles.forEach { file -> appendLine("  - ${file.filePath} (${file.reason})") }
                }
            }
        outputFile.writeText(content)
    }
}
