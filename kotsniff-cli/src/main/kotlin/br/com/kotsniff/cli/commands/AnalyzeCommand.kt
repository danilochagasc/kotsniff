package br.com.kotsniff.cli.commands

import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.cli.report.HtmlReportGenerator
import br.com.kotsniff.cli.report.JsonReportGenerator
import br.com.kotsniff.cli.report.MarkdownReportGenerator
import br.com.kotsniff.cli.report.TextReportGenerator
import br.com.kotsniff.cli.runner.KotSniffRunner
import br.com.kotsniff.cli.ui.TerminalPrinter
import br.com.kotsniff.core.models.DetectionResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
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
        name = "kotsniff",
        help = "A Kotlin test smells detector for the Kotest framework.",
    ) {
    private val path by option("-p", "--path", help = "Target directory to analyze").default(".")
    private val format by option("-f", "--format", help = "Output format").choice("markdown", "json", "text", "html").default("markdown")
    private val output by option("-o", "--output", help = "Output file path")
    private val showDetails by option("--show-details", help = "Print individual test smells to the terminal").flag(default = false)
    private val maxAssertions by option("--max-assertions", help = "Maximum allowed assertions per test block").int().default(3)

    override fun run() {
        val printer = TerminalPrinter { msg, err -> echo(msg, err = err) }
        printer.printStart()

        val targetDir = File(path)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            printer.printError("Invalid target directory: $path")
            throw ProgramResult(1)
        }

        val engine = KotSniffEngine.createDefault(maxAssertions)
        val runner = KotSniffRunner(engine)

        var totalFiles = 0
        var totalLoc = 0
        var results = emptyList<DetectionResult>()
        var failedFiles = emptyList<String>()

        val timeTakenMs =
            measureTimeMillis {
                val summary = runner.scanDirectory(targetDir)
                totalFiles = summary.totalFilesScanned
                totalLoc = summary.totalLinesOfCode
                results = summary.smells
                failedFiles = summary.failedFiles
            }

        printer.printScanSummary(totalFiles, timeTakenMs)

        if (showDetails) {
            printer.printResults(results)
        } else if (results.isNotEmpty()) {
            printer.printError("Found ${results.size} test smells! Run with --show-details to see them in the terminal.")
        }

        val outputFile = resolveOutputFile(targetDir)
        
        val smellsByType = results.groupingBy { it.smellType.displayName }.eachCount()

        val report =
            Report(
                totalFilesScanned = totalFiles,
                totalLinesOfCode = totalLoc,
                timeTakenMs = timeTakenMs,
                smellsFound = results.size,
                smellsByType = smellsByType,
                smells = results,
                failedFiles = failedFiles,
            )

        val generator =
            when (format) {
                "markdown" -> MarkdownReportGenerator()
                "text" -> TextReportGenerator()
                "html" -> HtmlReportGenerator()
                else -> JsonReportGenerator()
            }
        generator.generate(report, outputFile, targetDir)

        printer.printReportLocation(outputFile.absolutePath)
        printer.printFailedFilesWarning(failedFiles)

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
                    "html" -> "html"
                    else -> "txt"
                }
            File(targetDir, "kotsniff-report.$extension")
        }
}
