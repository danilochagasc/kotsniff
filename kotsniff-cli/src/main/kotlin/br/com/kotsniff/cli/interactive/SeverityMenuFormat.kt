package br.com.kotsniff.cli.interactive

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell

/**
 * Pure formatting for the interactive severity menu, kept separate from the
 * terminal so it can be unit-tested.
 *
 * Output is **ASCII only** and column-aligned so each entry fits on a single
 * line (previous versions used a title + description pair, which pushed every
 * rule onto two lines and made navigation look like it was skipping values).
 */
object SeverityMenuFormat {
    private const val NAME_WIDTH = 24
    private const val ID_WIDTH = 24
    private const val SEVERITY_WIDTH = 8

    /** One line per rule: `Rule name  RULE_ID  severity  origin`. */
    fun ruleLine(
        rule: TestSmell,
        severity: Severity,
        overridden: Boolean,
    ): String {
        val origin = if (overridden) "override" else "default"
        return pad(rule.ruleName, NAME_WIDTH) + pad(rule.name, ID_WIDTH) +
            pad(severity.label, SEVERITY_WIDTH) + origin
    }

    /** One line per severity option, marking the rule's current/default value. */
    fun severityLine(
        severity: Severity,
        current: Boolean,
        isDefault: Boolean,
    ): String {
        val marker = if (current) "[x]" else "[ ]"
        val note = if (isDefault) "  (default)" else ""
        return "$marker ${severity.label}$note"
    }

    private fun pad(
        value: String,
        width: Int,
    ): String = if (value.length >= width) "$value " else value + " ".repeat(width - value.length)
}
