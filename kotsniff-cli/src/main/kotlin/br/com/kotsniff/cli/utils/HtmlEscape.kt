package br.com.kotsniff.cli.utils

/**
 * Contextual escaping helpers for building HTML and inline JavaScript safely.
 *
 * The analysed source code (and file paths derived from the user's project) is
 * untrusted input: it may contain `&`, `<`, `>`, quotes or even `</script>`.
 * Every dynamic value embedded into a report must go through one of these
 * helpers instead of ad-hoc `replace` chains.
 */
object HtmlEscape {
    /** Escapes text for HTML element content and double/single-quoted attributes. */
    fun escape(value: String): String =
        buildString(value.length) {
            for (c in value) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(c)
                }
            }
        }

    /** Escapes a JavaScript/JSON string literal (without the surrounding quotes). */
    fun escapeJs(value: String): String =
        buildString(value.length) {
            for (c in value) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\'' -> append("\\'")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '<' -> append("\\u003c")
                    '>' -> append("\\u003e")
                    '&' -> append("\\u0026")
                    else -> append(c)
                }
            }
        }
}
