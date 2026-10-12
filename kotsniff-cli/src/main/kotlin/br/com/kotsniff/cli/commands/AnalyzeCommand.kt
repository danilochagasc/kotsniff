package br.com.kotsniff.cli.commands

import br.com.kotsniff.analyzer.AnalysisSettings
import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.analyzer.utils.NumericLiterals
import br.com.kotsniff.cli.config.ConfigLoader
import br.com.kotsniff.cli.config.FileConfig
import br.com.kotsniff.cli.interactive.SeverityConfigSession
import br.com.kotsniff.cli.interactive.SeverityConfigurator
import br.com.kotsniff.cli.interactive.SeverityMenuControl
import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.cli.models.ScanSummary
import br.com.kotsniff.cli.report.HtmlReportGenerator
import br.com.kotsniff.cli.report.JsonReportGenerator
import br.com.kotsniff.cli.report.MarkdownReportGenerator
import br.com.kotsniff.cli.report.ReportGenerator
import br.com.kotsniff.cli.report.SarifReportGenerator
import br.com.kotsniff.cli.report.TextReportGenerator
import br.com.kotsniff.cli.runner.KotSniffRunner
import br.com.kotsniff.cli.ui.TerminalPrinter
import br.com.kotsniff.core.KotsniffVersion
import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.Terminal
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Clikt command that orchestrates the KotSniff analysis pipeline.
 *
 * Delegates presentation to [TerminalPrinter], report persistence to the
 * `ReportGenerator` implementations, and analysis to [KotSniffRunner].
 *
 * Exit codes:
 * - `0` analysis succeeded and no smells were found (or `--no-fail` was given);
 * - `1` smells were found and `--no-fail` was not given;
 * - `2` the command could not run (invalid target, bad configuration).
 */
class AnalyzeCommand :
    CliktCommand(
        name = "kotsniff",
        help = "A Kotlin test smells detector for the Kotest framework.",
    ) {
    private val path by option("-p", "--path", help = "Target directory to analyze").default(".")
    private val format by option(
        "-f",
        "--format",
        help = "Output format",
    ).choice("markdown", "json", "text", "html", "sarif").default("markdown")
    private val output by option("-o", "--output", help = "Output file path")
    private val showDetails by option("--show-details", help = "Print individual test smells to the terminal").flag(default = false)
    private val maxAssertions by option("--max-assertions", help = "Maximum allowed assertions per test block").int()
    private val reportDelay by option("--report-delay", help = "Report delay(...) calls as Sleepy Test").flag(default = false)
    private val exclude by option("-x", "--exclude", help = "Glob pattern of files to exclude (repeatable)").multiple()
    private val disableRule by option("--disable-rule", help = "Rule id or name to disable (repeatable)").multiple()
    private val severity by option(
        "--severity",
        help = "Override a rule severity: RULE=LEVEL, e.g. MAGIC_NUMBER_TEST=high (repeatable)",
    ).multiple()
    private val configureSeverity by option(
        "--configure-severity",
        help = "Interactively configure rule severities and save them to kotsniff.json",
    ).flag(default = false)
    private val listRules by option("--list-rules", help = "List rules with their default severity and exit").flag(default = false)
    private val allowedNumber by option(
        "--allowed-number",
        help = "Numeric literal to also treat as non-magic (repeatable; adds to the default allowlist)",
    ).multiple()
    private val progress by option("--progress", help = "Show a live progress line during long analyses").flag(default = false)
    private val reportSource by option(
        "--report-source",
        help = "HTML source detail: full file or snippet only",
    ).choice("full", "snippet").default("full")
    private val reportMaxLines by option(
        "--report-max-lines",
        help = "Max lines embedded per file before context windowing (HTML)",
    ).int().default(2000)
    private val noFail by option("--no-fail", help = "Exit with code 0 even when smells are found").flag(default = false)
    private val noColor by option("--no-color", help = "Disable ANSI colors in the terminal output").flag(default = false)
    private val quiet by option("-q", "--quiet", help = "Only print errors and the report path").flag(default = false)
    private val configOption by option("--config", help = "Path to a kotsniff.json configuration file")
    private val showVersion by option("--version", help = "Show the KotSniff version and exit").flag(default = false)

    override fun run() {
        if (showVersion) {
            echo("KotSniff ${KotsniffVersion.VERSION}")
            throw ProgramResult(ExitCodes.OK)
        }

        if (listRules) {
            printRules()
            throw ProgramResult(ExitCodes.OK)
        }

        if (configureSeverity) {
            configureSeveritiesInteractively()
            throw ProgramResult(ExitCodes.OK)
        }

        val printer =
            TerminalPrinter(
                echo = { msg, err -> echo(msg, err = err) },
                rawEcho = { text -> echo(text, err = true, trailingNewline = false) },
                useColor = !noColor,
                quiet = quiet,
            )
        printer.printStart()

        val targetDir = File(path)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            printer.printError("Invalid target directory: $path")
            throw ProgramResult(ExitCodes.ERROR)
        }

        val config = loadConfig(printer, targetDir)

        val disabledRules = resolveDisabledRules(printer, disableRule + (config?.disabledRules ?: emptyList()))
        val extraAllowedNumbers =
            when {
                allowedNumber.isNotEmpty() -> allowedNumber.toSet()
                config?.allowedNumbers != null -> config.allowedNumbers.toSet()
                else -> emptySet()
            }
        val allowedNumbers = NumericLiterals.DEFAULT_ALLOWED + extraAllowedNumbers
        val severityOverrides =
            try {
                resolveSeverityOverrides(printer, config?.severities ?: emptyMap(), severity)
            } catch (e: IllegalArgumentException) {
                printer.printError(e.message ?: "Invalid severity configuration")
                throw ProgramResult(ExitCodes.ERROR)
            }
        val settings =
            AnalysisSettings(
                disabledRules = disabledRules,
                maxAssertions = maxAssertions ?: config?.maxAssertions ?: 3,
                reportDelay = reportDelay || (config?.reportDelay ?: false),
                allowedNumbers = allowedNumbers,
                severityOverrides = severityOverrides,
            )
        val excludes = exclude + (config?.exclude ?: emptyList())

        val engine = KotSniffEngine.createDefault(settings)
        val runner = KotSniffRunner(engine)

        val onProgress: ((Int, Int) -> Unit)? =
            if (progress && !quiet) {
                { processed, total -> printer.printProgress(processed, total) }
            } else {
                null
            }

        var summary: ScanSummary? = null
        val timeTakenMs = measureTimeMillis { summary = runner.scanDirectory(targetDir, excludes, onProgress) }
        printer.finishProgress()
        val scan = summary ?: ScanSummary(0, 0, 0, emptyList(), emptyList())

        printer.printScanSummary(
            totalFiles = scan.totalFilesScanned,
            ignoredFiles = scan.ignoredFiles,
            failedFiles = scan.failedFiles.size,
            timeTakenMs = timeTakenMs,
        )

        if (showDetails) {
            printer.printResults(scan.diagnostics)
        } else if (scan.diagnostics.isNotEmpty()) {
            printer.printWarning("Found ${scan.diagnostics.size} test smells! Run with --show-details to see them in the terminal.")
        }

        val outputFile = resolveOutputFile(targetDir)
        val report = buildReport(targetDir, scan, timeTakenMs)
        reportGenerator().generate(report, outputFile, targetDir)

        printer.printReportLocation(outputFile.absolutePath)
        printer.printFailedFiles(scan.failedFiles)

        if (scan.diagnostics.isNotEmpty() && !noFail) {
            throw ProgramResult(ExitCodes.FINDINGS)
        }
    }

    private fun loadConfig(
        printer: TerminalPrinter,
        targetDir: File,
    ): FileConfig? {
        val configFile = configOption?.let { File(it) } ?: File(targetDir, "kotsniff.json")
        return try {
            ConfigLoader.load(configFile)
        } catch (e: IllegalArgumentException) {
            printer.printError(e.message ?: "Invalid configuration")
            throw ProgramResult(ExitCodes.ERROR)
        }
    }

    private fun resolveDisabledRules(
        printer: TerminalPrinter,
        raw: List<String>,
    ): Set<TestSmell> =
        raw
            .filter { it.isNotBlank() }
            .mapNotNull { id ->
                val resolved = TestSmell.fromId(id)
                if (resolved == null) {
                    printer.printWarning("Unknown rule '$id' ignored. Known rules: ${TestSmell.entries.joinToString { it.name }}")
                }
                resolved
            }.toSet()

    /**
     * Resolves per-rule severity overrides with precedence **CLI > config > default**.
     *
     * @throws IllegalArgumentException on an invalid `RULE=LEVEL` entry or an
     *   unknown severity level.
     */
    private fun resolveSeverityOverrides(
        printer: TerminalPrinter,
        configRaw: Map<String, String>,
        cliRaw: List<String>,
    ): Map<TestSmell, Severity> {
        val result = linkedMapOf<TestSmell, Severity>()

        fun apply(
            source: String,
            pairs: List<Pair<String, String>>,
        ) {
            pairs.forEach { (ruleId, levelId) ->
                if (ruleId.isBlank()) return@forEach
                val rule = TestSmell.fromId(ruleId)
                if (rule == null) {
                    printer.printWarning(
                        "Unknown rule '$ruleId' in $source; ignored. Known rules: ${TestSmell.entries.joinToString { it.name }}",
                    )
                    return@forEach
                }
                val severity = Severity.fromId(levelId)
                if (severity == null) {
                    throw IllegalArgumentException(
                        "Invalid severity '$levelId' for '$ruleId' ($source). Valid values: ${Severity.entries.joinToString { it.label }}.",
                    )
                }
                result[rule] = severity
            }
        }

        apply("config", configRaw.map { it.key to it.value })
        apply(
            "--severity",
            cliRaw.filter { it.isNotBlank() }.map { entry ->
                val separator = entry.indexOf('=')
                if (separator <= 0 || separator == entry.length - 1) {
                    throw IllegalArgumentException("Invalid --severity '$entry'. Expected RULE=LEVEL, e.g. MAGIC_NUMBER_TEST=high.")
                }
                entry.substring(0, separator).trim() to entry.substring(separator + 1).trim()
            },
        )
        return result
    }

    private fun printRules() {
        echo("Rules (id | name | default severity):")
        TestSmell.entries.forEach { rule ->
            echo("  ${rule.name.padEnd(24)} ${rule.ruleName.padEnd(26)} ${rule.defaultSeverity.label}")
        }
    }

    private fun configureSeveritiesInteractively() {
        val terminal = Terminal()
        val configFile = configOption?.let { File(it) } ?: File(path, "kotsniff.json")
        if (System.console() == null) {
            echo(
                "--configure-severity needs an interactive terminal. Use --severity RULE=LEVEL instead.",
                err = true,
            )
            throw ProgramResult(ExitCodes.ERROR)
        }

        val existing =
            try {
                ConfigLoader.load(configFile)
            } catch (e: IllegalArgumentException) {
                echo(e.message ?: "Invalid configuration file", err = true)
                throw ProgramResult(ExitCodes.ERROR)
            }

        val initial = parseSeverityMap(existing?.severities ?: emptyMap())
        val session = SeverityConfigSession(initial)
        val outcome = SeverityConfigurator(terminal).run(session)

        if (outcome == SeverityMenuControl.Outcome.SAVE) {
            val updated = (existing ?: FileConfig()).copy(severities = session.toConfigMap())
            ConfigLoader.save(configFile, updated)
            echo("Saved severities to ${configFile.absolutePath}")
        } else {
            echo("No changes saved.")
        }
    }

    private fun parseSeverityMap(raw: Map<String, String>): Map<TestSmell, Severity> {
        val result = linkedMapOf<TestSmell, Severity>()
        raw.forEach { (ruleId, levelId) ->
            val rule = TestSmell.fromId(ruleId) ?: return@forEach
            Severity.fromId(levelId)?.let { result[rule] = it }
        }
        return result
    }

    private fun buildReport(
        targetDir: File,
        summary: ScanSummary,
        timeTakenMs: Long,
    ): Report {
        val severityCounts = summary.diagnostics.groupingBy { it.severity.label }.eachCount()
        val byType = summary.diagnostics.groupingBy { it.ruleName }.eachCount()
        return Report(
            toolVersion = KotsniffVersion.VERSION,
            projectName = targetDir.canonicalFile.name,
            totalFilesScanned = summary.totalFilesScanned,
            totalLinesOfCode = summary.totalLinesOfCode,
            ignoredFiles = summary.ignoredFiles,
            timeTakenMs = timeTakenMs,
            smellsFound = summary.diagnostics.size,
            severityCounts = severityCounts,
            smellsByType = byType,
            smells = summary.diagnostics,
            failedFiles = summary.failedFiles,
        )
    }

    private fun reportGenerator(): ReportGenerator =
        when (format) {
            "markdown" -> {
                MarkdownReportGenerator()
            }

            "text" -> {
                TextReportGenerator()
            }

            "html" -> {
                HtmlReportGenerator(
                    sourceMode =
                        if (reportSource == "snippet") {
                            HtmlReportGenerator.SourceMode.SNIPPET
                        } else {
                            HtmlReportGenerator.SourceMode.FULL
                        },
                    maxFileLines = reportMaxLines,
                )
            }

            "sarif" -> {
                SarifReportGenerator()
            }

            else -> {
                JsonReportGenerator()
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
                    "sarif" -> "sarif"
                    else -> "txt"
                }
            File(targetDir, "kotsniff-report.$extension")
        }

    /** Exit codes used by the CLI. */
    object ExitCodes {
        const val OK = 0
        const val FINDINGS = 1
        const val ERROR = 2
    }
}
