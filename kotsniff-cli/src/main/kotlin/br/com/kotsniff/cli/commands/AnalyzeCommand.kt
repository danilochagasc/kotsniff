package br.com.kotsniff.cli.commands

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.cli.report.JsonReportGenerator
import br.com.kotsniff.cli.report.MarkdownReportGenerator
import br.com.kotsniff.cli.report.TextReportGenerator
import br.com.kotsniff.cli.runner.KotSniffRunner
import br.com.kotsniff.cli.ui.TerminalPrinter
import br.com.kotsniff.core.models.DetectionResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Clikt command that orchestrates the KotSniff analysis pipeline.
 *
 * Delegates presentation to [TerminalPrinter], report persistence to
 * [JsonReportGenerator]/[TextReportGenerator], and analysis to [KotSniffRunner].
 */
class AnalyzeCommand :
    CliktCommand(
        name = "kotsniff-cli",
        help = "Analyzes Kotest files for test smells",
    ) {
    private val path by option("-p", "--path", help = "Target directory to analyze").default(".")
    private val format by option("-f", "--format", help = "Output format").choice("markdown", "json", "text").default("markdown")
    private val output by option("-o", "--output", help = "Output file path")

    override fun run() {
        val printer = TerminalPrinter { msg, err -> echo(msg, err = err) }
        printer.printStart()

        val targetDir = File(path)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            printer.printError("Invalid target directory: $path")
            throw ProgramResult(1)
        }

        val engine = KotSniffEngine.createDefault()
        val runner = KotSniffRunner(engine)

        var totalFiles = 0
        val results: List<DetectionResult>

        val timeTakenMs =
            measureTimeMillis {
                val (filesScanned, smells) = runner.scanDirectory(targetDir)
                totalFiles = filesScanned
                results = smells
            }

        printer.printScanSummary(totalFiles, timeTakenMs)
        printer.printResults(results)

        val outputFile = resolveOutputFile(targetDir)

        val report =
            Report(
                totalFilesScanned = totalFiles,
                timeTakenMs = timeTakenMs,
                smellsFound = results.size,
                smells = results,
            )

        val generator =
            when (format) {
                "markdown" -> MarkdownReportGenerator()
                "text" -> TextReportGenerator()
                else -> JsonReportGenerator()
            }
        generator.generate(report, outputFile)

        printer.printReportLocation(outputFile.absolutePath)

        if (results.isNotEmpty()) {
            throw ProgramResult(1)
        }
    }

    private fun resolveOutputFile(targetDir: File): File =
        if (output != null) {
            File(output!!)
        } else {
            val extension =
                when (format) {
                    "markdown" -> "md"
                    "json" -> "json"
                    else -> "txt"
                }
            File(targetDir, "kotsniff-report.$extension")
        }
}
