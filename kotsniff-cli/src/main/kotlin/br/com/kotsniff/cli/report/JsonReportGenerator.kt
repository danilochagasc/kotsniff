package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Generates a structured JSON report using kotlinx-serialization.
 */
class JsonReportGenerator : ReportGenerator {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    override fun generate(
        report: Report,
        outputFile: File,
        targetDir: File,
    ) {
        outputFile.writeText(json.encodeToString(report))
    }
}
