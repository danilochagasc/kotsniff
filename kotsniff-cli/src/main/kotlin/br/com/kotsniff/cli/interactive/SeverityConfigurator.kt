package br.com.kotsniff.cli.interactive

import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.terminal.Terminal

/**
 * Drives [SeverityMenuControl] on a real terminal: enters raw mode, reads key
 * events, redraws the frame in place, and highlights the selected row.
 *
 * Using Mordant's low-level key events (instead of the higher-level
 * `interactiveSelectList`) lets us bind **Esc** explicitly and show a
 * confirmation dialog when the user leaves with pending changes.
 */
class SeverityConfigurator(
    private val terminal: Terminal,
) {
    fun run(session: SeverityConfigSession): SeverityMenuControl.Outcome {
        val control = SeverityMenuControl(session)
        terminal.cursor.hide(showOnExit = true)
        var height = 0
        try {
            height = draw(control.lines(), height)
            return terminal.receiveKeyEvents { event ->
                val outcome = control.handle(normalizeKey(event.key, event.isCtrlC))
                if (outcome != null) {
                    InputReceiver.Status.Finished(outcome)
                } else {
                    height = draw(control.lines(), height)
                    InputReceiver.Status.Continue
                }
            }
        } finally {
            terminal.cursor.show()
            terminal.println()
        }
    }

    private fun normalizeKey(
        key: String,
        ctrlC: Boolean,
    ): String =
        when {
            ctrlC -> SeverityMenuControl.KEY_ESC
            key == "Up" -> SeverityMenuControl.KEY_UP
            key == "Down" -> SeverityMenuControl.KEY_DOWN
            else -> key
        }

    /**
     * Redraws the frame in place: moves up over the previous frame, clears each
     * line and prints the new one. Returns the number of lines drawn so the next
     * frame knows how far to move up.
     */
    private fun draw(
        lines: List<MenuLine>,
        printedLines: Int,
    ): Int {
        if (printedLines > 0) {
            terminal.cursor.move { up(printedLines) }
        }
        val count = maxOf(printedLines, lines.size)
        for (i in 0 until count) {
            terminal.cursor.move {
                startOfLine()
                clearLine()
            }
            val line = lines.getOrNull(i)
            val text =
                when {
                    line == null -> ""
                    line.selected -> "\u001B[7m${line.text}\u001B[0m"
                    else -> line.text
                }
            terminal.rawPrint(text + "\n")
        }
        return count
    }
}
