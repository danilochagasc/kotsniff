package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.cli.utils.formatCodeSnippet
import java.io.File

/**
 * Generates a human-readable Markdown report.
 */
class MarkdownReportGenerator : ReportGenerator {
    override fun generate(
        report: Report,
        outputFile: File,
        targetDir: File,
    ) {
        val content =
            buildString {
                appendLine("# KotSniff Report: ${report.projectName}")
                appendLine()
                appendLine("_Tool version: ${report.toolVersion} · schema v${report.schemaVersion}_")
                appendLine()
                appendLine("## Summary")
                appendLine("- **Files scanned:** ${report.totalFilesScanned} (${report.ignoredFiles} ignored)")
                appendLine("- **Lines of code:** ${report.totalLinesOfCode}")
                appendLine("- **Time taken:** ${report.timeTakenMs}ms")
                appendLine("- **Smells found:** ${report.smellsFound}")
                appendLine()

                if (report.severityCounts.isNotEmpty()) {
                    appendLine("### By severity")
                    report.severityCounts.forEach { (severity, count) -> appendLine("- $count **$severity**") }
                    appendLine()
                }

                if (report.smellsByType.isNotEmpty()) {
                    appendLine("### Breakdown by rule")
                    report.smellsByType.forEach { (type, count) -> appendLine("- $count **$type**") }
                    appendLine()
                }

                if (report.smells.isEmpty()) {
                    appendLine("🎉 **Awesome! No test smells found!**")
                } else {
                    appendLine("## Details")
                    appendLine()

                    report.smells
                        .groupBy { it.location.filePath }
                        .forEach { (file, smells) ->
                            appendLine("### 📁 $file")
                            appendLine()
                            smells.forEach { smell ->
                                appendLine(
                                    "- **[${smell.ruleName}]** (${smell.severity.label}) line ${smell.location.line}: ${smell.message}",
                                )
                                appendLine("```kotlin")
                                val snippet =
                                    smell.codeSnippet.formatCodeSnippet().lines().joinToString("\n") {
                                        if (it.startsWith("    ")) it.substring(4) else it
                                    }
                                appendLine(snippet)
                                appendLine("```")
                                smell.suggestion?.let { appendLine("  > $it") }
                                appendLine()
                            }
                        }
                }

                if (report.failedFiles.isNotEmpty()) {
                    appendLine("## Failed to analyse")
                    appendLine("The following files could not be analysed:")
                    appendLine()
                    report.failedFiles.forEach { file -> appendLine("- `${file.filePath}` (${file.reason})") }
                    appendLine()
                }
            }
        outputFile.writeText(content)
    }
}
