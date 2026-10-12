package br.com.kotsniff.cli.utils

/**
 * A tiny, dependency-free Kotlin syntax highlighter used by the HTML report.
 *
 * It is deliberately conservative: it colours comments, string/char literals,
 * numbers, annotations, keywords and (capitalised) type names, and leaves
 * everything else untouched. Multi-line block comments and raw strings are
 * tracked across lines via [State], so highlighting stays correct for the whole
 * file.
 *
 * Safety: each token's text is escaped with [HtmlEscape], so untrusted source
 * can never break out of the span or inject markup.
 *
 * Known limitations (documented, acceptable for a viewer): Kotlin's *nested*
 * block comments are not tracked, and `"${...}"` interpolation is coloured as
 * part of the enclosing string.
 */
object KotlinHighlighter {
    private val KEYWORDS =
        setOf(
            "package",
            "import",
            "class",
            "interface",
            "object",
            "fun",
            "val",
            "var",
            "if",
            "else",
            "when",
            "for",
            "while",
            "do",
            "try",
            "catch",
            "finally",
            "throw",
            "return",
            "break",
            "continue",
            "is",
            "in",
            "as",
            "as?",
            "this",
            "super",
            "null",
            "true",
            "false",
            "typealias",
            "constructor",
            "init",
            "by",
            "where",
            "out",
            "override",
            "private",
            "public",
            "protected",
            "internal",
            "abstract",
            "final",
            "open",
            "sealed",
            "data",
            "enum",
            "annotation",
            "companion",
            "inline",
            "noinline",
            "crossinline",
            "reified",
            "suspend",
            "tailrec",
            "operator",
            "infix",
            "const",
            "lateinit",
            "vararg",
            "external",
            "expect",
            "actual",
            "context",
            "get",
            "set",
            "field",
            "to",
            "until",
            "downTo",
            "step",
            "yield",
        )

    /** Highlighting state carried from one line to the next. */
    class State {
        internal var inBlockComment: Boolean = false
        internal var inRawString: Boolean = false
    }

    /** Returns one HTML fragment per input line (no line wrapper, no numbering). */
    fun highlight(lines: List<String>): List<String> {
        val state = State()
        return lines.map { highlightLine(it, state) }
    }

    private fun highlightLine(
        line: String,
        state: State,
    ): String {
        val out = StringBuilder()
        val plain = StringBuilder()

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                out.append(HtmlEscape.escape(plain.toString()))
                plain.setLength(0)
            }
        }

        fun token(
            kind: String,
            text: String,
        ) {
            flushPlain()
            out
                .append("<span class=\"")
                .append(kind)
                .append("\">")
                .append(HtmlEscape.escape(text))
                .append("</span>")
        }

        var i = 0
        val n = line.length

        if (state.inBlockComment) {
            val end = line.indexOf("*/")
            if (end == -1) {
                token("tok-comment", line)
                return out.toString()
            }
            token("tok-comment", line.substring(0, end + 2))
            state.inBlockComment = false
            i = end + 2
        }

        if (state.inRawString) {
            val end = line.indexOf("\"\"\"", i)
            if (end == -1) {
                token("tok-string", line.substring(i))
                return out.toString()
            }
            token("tok-string", line.substring(i, end + 3))
            state.inRawString = false
            i = end + 3
        }

        while (i < n) {
            val c = line[i]
            when {
                c == '/' && i + 1 < n && line[i + 1] == '/' -> {
                    token("tok-comment", line.substring(i))
                    i = n
                }

                c == '/' && i + 1 < n && line[i + 1] == '*' -> {
                    val end = line.indexOf("*/", i + 2)
                    if (end == -1) {
                        token("tok-comment", line.substring(i))
                        state.inBlockComment = true
                        i = n
                    } else {
                        token("tok-comment", line.substring(i, end + 2))
                        i = end + 2
                    }
                }

                c == '"' && i + 2 < n && line[i + 1] == '"' && line[i + 2] == '"' -> {
                    val end = line.indexOf("\"\"\"", i + 3)
                    if (end == -1) {
                        token("tok-string", line.substring(i))
                        state.inRawString = true
                        i = n
                    } else {
                        token("tok-string", line.substring(i, end + 3))
                        i = end + 3
                    }
                }

                c == '"' -> {
                    val end = stringEnd(line, i)
                    token("tok-string", line.substring(i, end + 1))
                    i = end + 1
                }

                c == '\'' -> {
                    val end = charEnd(line, i)
                    token("tok-string", line.substring(i, end + 1))
                    i = end + 1
                }

                c == '@' -> {
                    var j = i + 1
                    while (j < n && (line[j].isLetterOrDigit() || line[j] == '_' || line[j] == ':')) j++
                    token("tok-annotation", line.substring(i, j))
                    i = j
                }

                c.isDigit() -> {
                    var j = i + 1
                    while (j < n && (line[j].isLetterOrDigit() || line[j] == '_' || line[j] == '.')) j++
                    token("tok-number", line.substring(i, j))
                    i = j
                }

                c.isLetter() || c == '_' -> {
                    var j = i + 1
                    while (j < n && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                    val word = line.substring(i, j)
                    when {
                        word in KEYWORDS -> token("tok-keyword", word)
                        word[0] in 'A'..'Z' -> token("tok-type", word)
                        else -> plain.append(word)
                    }
                    i = j
                }

                else -> {
                    plain.append(c)
                    i++
                }
            }
        }

        flushPlain()
        return out.toString()
    }

    private fun stringEnd(
        line: String,
        start: Int,
    ): Int {
        var i = start + 1
        while (i < line.length) {
            val c = line[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == '"') return i
            i++
        }
        return line.length - 1
    }

    private fun charEnd(
        line: String,
        start: Int,
    ): Int {
        var i = start + 1
        while (i < line.length) {
            val c = line[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == '\'') return i
            i++
        }
        return line.length - 1
    }
}
