package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import java.io.File

/**
 * Strategy interface for generating analysis reports in different formats.
 */
interface ReportGenerator {
    fun generate(
        report: Report,
        outputFile: File,
    )
}
