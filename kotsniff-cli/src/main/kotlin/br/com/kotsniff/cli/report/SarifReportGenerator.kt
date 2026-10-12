package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.core.enums.Severity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File

/**
 * Generates a SARIF 2.1.0 report for interoperability with code-scanning tools
 * (e.g. GitHub Code Scanning) and IDEs.
 *
 * See https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
 */
class SarifReportGenerator : ReportGenerator {
    private val json = Json { prettyPrint = true }

    override fun generate(
        report: Report,
        outputFile: File,
        targetDir: File,
    ) {
        val usedRules = report.smells.map { it.ruleId }.distinct()

        val driver =
            buildJsonObject {
                put("name", "KotSniff")
                put("version", report.toolVersion)
                put("informationUri", "https://github.com/kotsniff")
                putJsonArray("rules") {
                    usedRules.forEach { rule ->
                        add(
                            buildJsonObject {
                                put("id", rule.name)
                                put("name", rule.ruleName)
                                putJsonObject("shortDescription") { put("text", rule.description) }
                                putJsonObject("defaultConfiguration") { put("level", sarifLevel(rule.defaultSeverity)) }
                            },
                        )
                    }
                }
            }

        val results =
            buildJsonArray {
                report.smells.forEach { smell ->
                    add(
                        buildJsonObject {
                            put("ruleId", smell.ruleId.name)
                            put("level", sarifLevel(smell.severity))
                            putJsonObject("message") { put("text", smell.message) }
                            putJsonArray("locations") {
                                add(
                                    buildJsonObject {
                                        putJsonObject("physicalLocation") {
                                            putJsonObject("artifactLocation") {
                                                put("uri", smell.location.filePath.replace('\\', '/'))
                                            }
                                            putJsonObject("region") {
                                                put("startLine", smell.location.line)
                                                put("startColumn", smell.location.column)
                                                put("endLine", smell.location.endLine)
                                                put("endColumn", smell.location.endColumn)
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
            }

        val sarif =
            buildJsonObject {
                put("\$schema", "https://json.schemastore.org/sarif-2.1.0.json")
                put("version", "2.1.0")
                putJsonArray("runs") {
                    add(
                        buildJsonObject {
                            putJsonObject("tool") { put("driver", driver) }
                            put("results", results)
                        },
                    )
                }
            }

        outputFile.writeText(json.encodeToString(sarif))
    }

    private fun sarifLevel(severity: Severity): String =
        when (severity) {
            Severity.HIGH -> "error"
            Severity.MEDIUM -> "warning"
            Severity.LOW, Severity.INFO -> "note"
        }
}
