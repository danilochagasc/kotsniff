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
                appendLine("# KotSniff Report")
                appendLine()
                appendLine("## Summary")
                appendLine("- **Files scanned:** ${report.totalFilesScanned}")
                appendLine("- **Time taken:** ${report.timeTakenMs}ms")
                appendLine("- **Smells found:** ${report.smellsFound}")
                appendLine()

                if (report.smells.isEmpty()) {
                    appendLine("🎉 **Awesome! No test smells found!**")
                    return@buildString
                }

                appendLine("## Details")
                appendLine()

                report.smells
                    .groupBy { it.location.filePath }
                    .forEach { (file, smells) ->
                        appendLine("### 📁 $file")
                        appendLine()
                        smells.forEach { smell ->
                            appendLine("- **[${smell.smellType}]** line ${smell.location.line}:")
                            appendLine("```kotlin")
                            // We don't necessarily need the 4-space prefix for markdown code blocks,
                            // but formatCodeSnippet() gives us a uniform baseline.
                            // Let's strip the exact 4-space prefix it adds to keep the block flush.
                            val snippet =
                                smell.codeSnippet.formatCodeSnippet().lines().joinToString("\n") {
                                    if (it.startsWith("    ")) it.substring(4) else it
                                }
                            appendLine(snippet)
                            appendLine("```")
                            appendLine()
                        }
                    }

                if (report.failedFiles.isNotEmpty()) {
                    appendLine("## Failed to Parse")
                    appendLine("The following files could not be analyzed due to syntax errors:")
                    appendLine()
                    report.failedFiles.forEach { file ->
                        appendLine("- `$file`")
                    }
                    appendLine()
                }
            }
        outputFile.writeText(content)
    }
}
