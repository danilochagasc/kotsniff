package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Generates a structured JSON report using kotlinx-serialization.
 */
class JsonReportGenerator : ReportGenerator {
    override fun generate(
        report: Report,
        outputFile: File,
    ) {
        val json = Json { prettyPrint = true }
        outputFile.writeText(json.encodeToString(report))
    }
}
