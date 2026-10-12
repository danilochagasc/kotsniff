package br.com.kotsniff.core.models

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell
import kotlinx.serialization.Serializable

/**
 * A single test smell occurrence, normalised so that **every** report format
 * (console, JSON, Markdown, text, HTML, SARIF) consumes the same model.
 *
 * @property ruleId The stable rule identifier.
 * @property ruleName Human-readable rule name (derived from [ruleId]).
 * @property severity Severity of the finding (derived from [ruleId] unless overridden).
 * @property message A short, actionable description of the problem.
 * @property description A longer explanation of why the smell matters.
 * @property suggestion An optional remediation hint.
 * @property location The exact source range where the smell was found.
 * @property codeSnippet The relevant source snippet.
 * @property metadata Optional rule-specific extra information.
 */
@Serializable
data class Diagnostic(
    val ruleId: TestSmell,
    val ruleName: String,
    val severity: Severity,
    val message: String,
    val description: String? = null,
    val suggestion: String? = null,
    val location: CodeLocation,
    val codeSnippet: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        /**
         * Creates a [Diagnostic] filling derived fields (rule name and severity)
         * from the [ruleId] catalogue, so detectors cannot diverge on them.
         */
        fun of(
            ruleId: TestSmell,
            location: CodeLocation,
            codeSnippet: String,
            message: String = ruleId.description,
            suggestion: String? = null,
            metadata: Map<String, String> = emptyMap(),
            severity: Severity = ruleId.defaultSeverity,
        ): Diagnostic =
            Diagnostic(
                ruleId = ruleId,
                ruleName = ruleId.ruleName,
                severity = severity,
                message = message,
                description = ruleId.description,
                suggestion = suggestion,
                location = location,
                codeSnippet = codeSnippet,
                metadata = metadata,
            )
    }
}
