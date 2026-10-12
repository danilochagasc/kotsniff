package br.com.kotsniff.cli.interactive

import br.com.kotsniff.core.enums.Severity
import br.com.kotsniff.core.enums.TestSmell

/** A single rendered menu line, flagged when it is the highlighted row. */
data class MenuLine(
    val text: String,
    val selected: Boolean,
)

/**
 * Pure state machine for the severity editor: no terminal, no I/O. Given a key
 * it mutates the current screen/cursor and returns an [Outcome] when the user
 * finishes, or `null` to keep going. [lines] renders the current screen.
 *
 * Screens:
 * - **Rules**: pick a rule (Enter), Save, or Cancel.
 * - **Severity**: choose a level for a rule (Enter) or go back (Esc).
 * - **Confirm**: shown when the user presses Esc with pending changes, asking
 *   to save-and-exit or exit-without-saving.
 */
class SeverityMenuControl(
    private val session: SeverityConfigSession,
    private val rules: List<TestSmell> = TestSmell.entries,
) {
    enum class Outcome {
        SAVE,
        CANCEL,
    }

    private sealed interface Screen {
        data object Rules : Screen

        data class Severity(
            val rule: TestSmell,
        ) : Screen

        data object Confirm : Screen
    }

    private var screen: Screen = Screen.Rules
    private var ruleCursor: Int = 0
    private var severityCursor: Int = 0
    private var confirmCursor: Int = 0

    private val ruleRowCount: Int get() = rules.size + 2 // + Save + Cancel
    private val severityRowCount: Int get() = Severity.entries.size + 1 // + Reset

    /** Applies a key press. Returns an [Outcome] when the session should end. */
    fun handle(key: String): Outcome? {
        when (val current = screen) {
            Screen.Rules -> handleRules(key)
            is Screen.Severity -> handleSeverity(key, current.rule)
            Screen.Confirm -> handleConfirm(key)
        }
        return pendingFinish
    }

    private var pendingFinish: Outcome? = null

    private fun handleRules(key: String) {
        when (key) {
            KEY_UP, "k" -> {
                ruleCursor = (ruleCursor - 1 + ruleRowCount) % ruleRowCount
            }

            KEY_DOWN, "j" -> {
                ruleCursor = (ruleCursor + 1) % ruleRowCount
            }

            KEY_ENTER -> {
                when {
                    ruleCursor in rules.indices -> openSeverity(rules[ruleCursor])
                    ruleCursor == rules.size -> pendingFinish = Outcome.SAVE
                    else -> requestFinish()
                }
            }

            KEY_ESC -> {
                requestFinish()
            }
        }
    }

    private fun handleSeverity(
        key: String,
        rule: TestSmell,
    ) {
        when (key) {
            KEY_UP, "k" -> {
                severityCursor = (severityCursor - 1 + severityRowCount) % severityRowCount
            }

            KEY_DOWN, "j" -> {
                severityCursor = (severityCursor + 1) % severityRowCount
            }

            KEY_ENTER -> {
                if (severityCursor == Severity.entries.size) {
                    session.reset(rule)
                } else {
                    session.set(rule, Severity.entries[severityCursor])
                }
                screen = Screen.Rules
            }

            KEY_ESC -> {
                screen = Screen.Rules
            }
        }
    }

    private fun handleConfirm(key: String) {
        when (key) {
            KEY_UP, KEY_DOWN, "k", "j" -> confirmCursor = 1 - confirmCursor
            KEY_ENTER -> pendingFinish = if (confirmCursor == 0) Outcome.SAVE else Outcome.CANCEL
            KEY_ESC -> screen = Screen.Rules
        }
    }

    private fun openSeverity(rule: TestSmell) {
        screen = Screen.Severity(rule)
        severityCursor = Severity.entries.indexOf(session.effective(rule)).coerceAtLeast(0)
    }

    /** Esc / Cancel with no pending changes ends the session; otherwise it asks. */
    private fun requestFinish() {
        if (session.hasChanges) {
            screen = Screen.Confirm
            confirmCursor = 0
        } else {
            pendingFinish = Outcome.CANCEL
        }
    }

    /** The lines to render for the current screen. */
    fun lines(): List<MenuLine> =
        when (val current = screen) {
            Screen.Rules -> rulesLines()
            is Screen.Severity -> severityLines(current.rule)
            Screen.Confirm -> confirmLines()
        }

    private fun rulesLines(): List<MenuLine> =
        buildList {
            add(MenuLine("KotSniff - configure severities", false))
            add(MenuLine("up/down: move   Enter: edit   Esc: save & exit", false))
            add(MenuLine("", false))
            rules.forEachIndexed { index, rule ->
                val selected = ruleCursor == index
                add(
                    MenuLine(
                        marker(selected) + SeverityMenuFormat.ruleLine(rule, session.effective(rule), session.isOverridden(rule)),
                        selected,
                    ),
                )
            }
            val saveSelected = ruleCursor == rules.size
            add(MenuLine(marker(saveSelected) + ">> Save and exit", saveSelected))
            val cancelSelected = ruleCursor == rules.size + 1
            add(MenuLine(marker(cancelSelected) + "<< Cancel", cancelSelected))
        }

    private fun severityLines(rule: TestSmell): List<MenuLine> =
        buildList {
            add(MenuLine("Severity for \"${rule.ruleName}\"", false))
            add(MenuLine("up/down: move   Enter: confirm   Esc: back", false))
            add(MenuLine("", false))
            Severity.entries.forEachIndexed { index, severity ->
                val selected = severityCursor == index
                add(
                    MenuLine(
                        marker(selected) +
                            SeverityMenuFormat.severityLine(
                                severity,
                                current = severity == session.effective(rule),
                                isDefault = severity == rule.defaultSeverity,
                            ),
                        selected,
                    ),
                )
            }
            val resetSelected = severityCursor == Severity.entries.size
            add(MenuLine(marker(resetSelected) + "<- Reset to default", resetSelected))
        }

    private fun confirmLines(): List<MenuLine> {
        val inner = BOX_WIDTH

        fun row(
            text: String,
            selected: Boolean,
        ) = MenuLine("| " + text.padEnd(inner) + " |", selected)

        return buildList {
            add(MenuLine("+" + "-".repeat(inner + 2) + "+", false))
            add(row("Unsaved changes", false))
            add(row("", false))
            add(row(marker(confirmCursor == 0) + "Save and exit", confirmCursor == 0))
            add(row(marker(confirmCursor == 1) + "Exit without saving", confirmCursor == 1))
            add(row("", false))
            add(row("Enter: select   Esc: back", false))
            add(MenuLine("+" + "-".repeat(inner + 2) + "+", false))
        }
    }

    private fun marker(selected: Boolean): String = if (selected) "> " else "  "

    companion object {
        const val KEY_UP = "ArrowUp"
        const val KEY_DOWN = "ArrowDown"
        const val KEY_ENTER = "Enter"
        const val KEY_ESC = "Escape"
        private const val BOX_WIDTH = 34
    }
}
