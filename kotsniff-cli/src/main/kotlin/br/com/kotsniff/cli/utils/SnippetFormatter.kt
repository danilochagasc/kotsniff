package br.com.kotsniff.cli.utils

/**
 * Formats a code snippet extracted from Kotlin PSI so that its indentation is uniform.
 * 
 * PSI's `expr.text` strips the leading whitespace of the first line because the whitespace 
 * is considered a separate PSI element. This function restores the visual alignment by 
 * calculating the baseline indentation from the subsequent lines.
 */
fun String.formatCodeSnippet(): String {
    val lines = this.lines()
    if (lines.size <= 1) return "    " + this.trim()

    val minIndent =
        lines
            .drop(1)
            .filter { it.isNotBlank() }
            .minOfOrNull { line -> line.takeWhile { it.isWhitespace() }.length } ?: 0

    return lines
        .mapIndexed { index, line ->
            if (index == 0) {
                "    $line"
            } else if (line.isNotBlank() && line.length >= minIndent) {
                "    ${line.substring(minIndent)}"
            } else {
                line
            }
        }.joinToString("\n")
}
