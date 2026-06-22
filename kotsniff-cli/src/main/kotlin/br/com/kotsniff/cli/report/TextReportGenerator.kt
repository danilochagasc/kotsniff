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
                appendLine("KotSniff Report")
                appendLine("Project: ${report.projectName}")
                appendLine("═══════════════════════════════════════")
                appendLine("Files scanned: ${report.totalFilesScanned}")
                appendLine("Lines of code: ${report.totalLinesOfCode}")
                appendLine("Time taken: ${report.timeTakenMs}ms")
                appendLine("Smells found: ${report.smellsFound}")
                if (report.smellsByType.isNotEmpty()) {
                    appendLine()
                    appendLine("Breakdown:")
                    report.smellsByType.forEach { (type, count) ->
                        appendLine("  - $count $type")
                    }
                }
                appendLine("═══════════════════════════════════════")

                if (report.smells.isEmpty()) {
                    appendLine("\nNo test smells found!")
                    return@buildString
                }

                report.smells
                    .groupBy { it.location.filePath }
                    .forEach { (file, smells) ->
                        appendLine("\nFile: $file")
                        smells.forEach { smell ->
                            appendLine(
                                "  - [${smell.smellType.displayName}] line ${smell.location.line}:\n${smell.codeSnippet.formatCodeSnippet()}",
                            )
                        }
                    }

                if (report.failedFiles.isNotEmpty()) {
                    appendLine("\nFailed to Parse:")
                    report.failedFiles.forEach { file ->
                        appendLine("  - $file")
                    }
                }
            }
        outputFile.writeText(content)
    }
}
