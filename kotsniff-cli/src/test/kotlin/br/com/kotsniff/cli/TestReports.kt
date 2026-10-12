package br.com.kotsniff.cli

import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.CodeLocation
import br.com.kotsniff.core.models.Diagnostic

/** Small builders shared by CLI tests. */
object TestReports {
    const val TOOL_VERSION = "0.2.0"

    fun diagnostic(
        path: String = "src/Test.kt",
        snippet: String = "if (true) println(1)",
        line: Int = 3,
    ): Diagnostic =
        Diagnostic.of(
            TestSmell.CONDITIONAL_TEST_LOGIC,
            CodeLocation(path, line, 5, line, 5),
            snippet,
        )

    fun single(
        diagnostic: Diagnostic,
        projectName: String = "sample-project",
    ): Report =
        Report(
            toolVersion = TOOL_VERSION,
            projectName = projectName,
            totalFilesScanned = 1,
            totalLinesOfCode = 10,
            ignoredFiles = 0,
            timeTakenMs = 1,
            smellsFound = 1,
            severityCounts = mapOf(diagnostic.severity.label to 1),
            smellsByType = mapOf(diagnostic.ruleName to 1),
            smells = listOf(diagnostic),
            failedFiles = emptyList(),
        )
}
