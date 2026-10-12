package br.com.kotsniff.cli.interactive

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell

/**
 * Pure state for the interactive severity configurator.
 *
 * Holds the per-rule overrides (initialised from an existing configuration),
 * exposes the effective severity of each rule, and produces the map that is
 * written to `kotsniff.json`. It has no terminal or I/O concerns, so it can be
 * unit-tested independently of the TUI.
 *
 * @param initial Overrides loaded from the existing configuration file.
 */
class SeverityConfigSession(
    initial: Map<TestSmell, Severity> = emptyMap(),
) {
    private val initialOverrides: Map<TestSmell, Severity> = initial.toMap()

    /** Current overrides (rules absent here use their default severity). */
    val overrides: Map<TestSmell, Severity> get() = current.toMap()
    private val current: MutableMap<TestSmell, Severity> = initial.toMutableMap()

    /** The severity that will apply to [rule] (override, else default). */
    fun effective(rule: TestSmell): Severity = current[rule] ?: rule.defaultSeverity

    /** Whether [rule] currently has an override. */
    fun isOverridden(rule: TestSmell): Boolean = current.containsKey(rule)

    /**
     * Sets [rule] to [severity]. Setting the default severity clears the override
     * so the rule follows the catalogue default again.
     */
    fun set(
        rule: TestSmell,
        severity: Severity,
    ) {
        if (severity == rule.defaultSeverity) current.remove(rule) else current[rule] = severity
    }

    /** Removes any override for [rule]. */
    fun reset(rule: TestSmell) {
        current.remove(rule)
    }

    /** Whether the current overrides differ from the initial ones. */
    val hasChanges: Boolean get() = current != initialOverrides

    /** Rule id -> severity label, sorted by rule id (stable file output). */
    fun toConfigMap(): Map<String, String> =
        current.entries
            .sortedBy { it.key.name }
            .associate { it.key.name to it.value.label }
}
