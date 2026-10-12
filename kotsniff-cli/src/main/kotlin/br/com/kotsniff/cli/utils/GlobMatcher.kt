package br.com.kotsniff.cli.utils

/**
 * Minimal glob matcher used for file exclusion.
 *
 * Supports `*` (any run of characters except `/`), `**` (any run of characters)
 * and `?` (a single character except `/`). Patterns are matched against paths
 * relative to the scanned root, using `/` as separator.
 */
class GlobMatcher(
    patterns: List<String>,
) {
    private val regexes: List<Regex> = patterns.filter { it.isNotBlank() }.map { toRegex(it) }

    fun matches(path: String): Boolean = regexes.any { it.matches(path) }

    companion object {
        private const val REGEX_SPECIALS = "/.()|^$@%{}[]+\\"

        fun toRegex(glob: String): Regex {
            val sb = StringBuilder("^")
            var i = 0
            while (i < glob.length) {
                val c = glob[i]
                if (c == '*') {
                    val isDouble = i + 1 < glob.length && glob[i + 1] == '*'
                    if (isDouble) {
                        // `**/` matches zero or more path segments; `**` matches anything.
                        if (i + 2 < glob.length && glob[i + 2] == '/') {
                            sb.append("(?:.*/)?")
                            i += 3
                            continue
                        }
                        sb.append(".*")
                        i += 2
                        continue
                    }
                    sb.append("[^/]*")
                    i++
                    continue
                }
                when {
                    c == '?' -> sb.append("[^/]")
                    c in REGEX_SPECIALS -> sb.append('\\').append(c)
                    else -> sb.append(c)
                }
                i++
            }
            sb.append('$')
            return Regex(sb.toString())
        }
    }
}
