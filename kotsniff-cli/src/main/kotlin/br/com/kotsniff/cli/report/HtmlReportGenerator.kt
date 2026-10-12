package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import br.com.kotsniff.cli.utils.HtmlEscape
import br.com.kotsniff.cli.utils.KotlinHighlighter
import br.com.kotsniff.core.models.Diagnostic
import java.io.File

/**
 * Generates a **self-contained** HTML report (no external CSS, fonts or
 * JavaScript) with two views:
 *
 * - **Files**: per-file navigation (sidebar) → per-file smell list → source with
 *   the selected smell's line(s) highlighted. The code scrolls inside its own
 *   container so the toolbar and the active-smell summary stay visible even for
 *   very large files; prev/next move across every visible smell.
 * - **List**: a flat, searchable/filterable card list.
 *
 * Security: every dynamic value is escaped for its output context, and the
 * generated HTML is produced server-side. The small client-side script only
 * reads `data-*` attributes and toggles visibility — **no dynamic data is ever
 * concatenated into JavaScript**, which removes the injection/stability issues
 * of the original implementation.
 *
 * @property sourceMode Whether to embed the analysed file contents ([SourceMode.FULL])
 *   or only the recorded snippets ([SourceMode.SNIPPET]).
 * @property maxFileLines Files longer than this are rendered as context windows
 *   around each smell instead of in full.
 */
class HtmlReportGenerator(
    private val sourceMode: SourceMode = SourceMode.FULL,
    private val maxFileLines: Int = DEFAULT_MAX_FILE_LINES,
) : ReportGenerator {
    /** How the source code is embedded in the report. */
    enum class SourceMode {
        FULL,
        SNIPPET,
    }

    override fun generate(
        report: Report,
        outputFile: File,
        targetDir: File,
    ) {
        val sorted =
            report.smells.sortedWith(
                compareBy({ it.location.filePath }, { it.location.line }, { it.location.column }),
            )
        // Keep a stable global index so the in-file smell list can reference entries.
        val indexed = sorted.mapIndexed { index, diagnostic -> index to diagnostic }
        val byFile = indexed.groupBy { it.second.location.filePath }
        val fileIndex = byFile.keys.withIndex().associate { it.value to it.index }
        val sources: Map<String, List<String>?> =
            byFile.keys.associateWith { path ->
                if (sourceMode == SourceMode.FULL) readLines(targetDir, path) else null
            }

        val html =
            buildString {
                appendLine("<!DOCTYPE html>")
                appendLine("<html lang=\"en\">")
                appendLine("<head>")
                appendLine("<meta charset=\"UTF-8\">")
                appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                appendLine("<title>KotSniff Report — ${HtmlEscape.escape(report.projectName)}</title>")
                appendLine("<style>")
                appendLine(CSS)
                appendLine("</style>")
                appendLine("</head>")
                appendLine("<body>")

                appendLine("<header class=\"topbar\">")
                appendLine("  <div class=\"brand\">")
                appendLine("    <span class=\"logo\">KS</span>")
                appendLine("    <div>")
                appendLine("      <h1>KotSniff Report</h1>")
                appendLine(
                    "      <p class=\"muted\">${HtmlEscape.escape(report.projectName)} · v${HtmlEscape.escape(report.toolVersion)}</p>",
                )
                appendLine("    </div>")
                appendLine("  </div>")
                appendLine("  <div class=\"stats\">")
                appendStat(report.smellsFound, "Smells")
                appendStat(report.totalFilesScanned, "Files")
                appendStat(report.totalLinesOfCode, "LOC")
                appendStat("${report.timeTakenMs}ms", "Time")
                appendLine("  </div>")
                appendLine("</header>")

                appendLine("<main>")

                if (report.severityCounts.isNotEmpty()) {
                    appendLine("<section class=\"chips\">")
                    report.severityCounts.entries.sortedByDescending { severityRank(it.key) }.forEach { (severity, count) ->
                        appendLine(
                            "  <span class=\"chip sev-${HtmlEscape.escape(
                                severity,
                            )}\">${HtmlEscape.escape(severity)}: <strong>$count</strong></span>",
                        )
                    }
                    appendLine("</section>")
                }

                if (report.smells.isEmpty()) {
                    appendLine("<section class=\"empty\">🎉 No test smells found.</section>")
                } else {
                    appendLine("<section class=\"controls\">")
                    appendLine(
                        "  <input id=\"search\" type=\"search\" placeholder=\"Search message, rule, file or code…\" aria-label=\"Search\">",
                    )
                    appendLine("  <select id=\"ruleFilter\" aria-label=\"Filter by rule\">")
                    appendLine("    <option value=\"\">All rules</option>")
                    usedRules(report).forEach { (id, name) ->
                        appendLine("    <option value=\"${HtmlEscape.escape(id)}\">${HtmlEscape.escape(name)}</option>")
                    }
                    appendLine("  </select>")
                    appendLine("  <select id=\"severityFilter\" aria-label=\"Filter by severity\">")
                    appendLine("    <option value=\"\">All severities</option>")
                    report.severityCounts.keys.sortedByDescending { severityRank(it) }.forEach { severity ->
                        appendLine("    <option value=\"${HtmlEscape.escape(severity)}\">${HtmlEscape.escape(severity)}</option>")
                    }
                    appendLine("  </select>")
                    appendLine("  <div class=\"view-toggle\" role=\"tablist\" aria-label=\"View\">")
                    appendLine("    <button id=\"viewFilesBtn\" class=\"tab active\" type=\"button\">Files</button>")
                    appendLine("    <button id=\"viewCardsBtn\" class=\"tab\" type=\"button\">List</button>")
                    appendLine("  </div>")
                    appendLine("  <span class=\"muted\" id=\"visibleCount\">${report.smellsFound}</span>")
                    appendLine("</section>")

                    appendLine("<div id=\"noMatches\" class=\"muted empty-search is-hidden\">No smells match the current filters.</div>")

                    appendLine(filesView(byFile, fileIndex, sources))
                    appendLine(cardsView(sorted))
                    appendLine(smellEntries(sorted, fileIndex))
                }

                if (report.failedFiles.isNotEmpty()) {
                    appendLine("<section class=\"failed\">")
                    appendLine("  <h2>⚠ Failed to analyse (${report.failedFiles.size})</h2>")
                    appendLine("  <ul>")
                    report.failedFiles.forEach { file ->
                        appendLine("    <li><code>${HtmlEscape.escape(file.filePath)}</code> — ${HtmlEscape.escape(file.reason)}</li>")
                    }
                    appendLine("  </ul>")
                    appendLine("</section>")
                }

                appendLine(
                    "<p class=\"muted foot\">Generated by KotSniff v${HtmlEscape.escape(
                        report.toolVersion,
                    )} · schema v${report.schemaVersion}</p>",
                )
                appendLine("</main>")

                appendLine("<script>")
                appendLine(SCRIPT)
                appendLine("</script>")
                appendLine("</body>")
                appendLine("</html>")
            }

        outputFile.writeText(html)
    }

    // ------------------------------------------------------------------ views

    private fun StringBuilder.filesView(
        byFile: Map<String, List<Pair<Int, Diagnostic>>>,
        fileIndex: Map<String, Int>,
        sources: Map<String, List<String>?>,
    ) {
        appendLine("<div id=\"view-files\" class=\"view\">")
        appendLine("<aside class=\"sidebar\">")
        appendLine("  <h2 class=\"side-title\">Files with smells</h2>")
        byFile.forEach { (path, entries) ->
            val idx = fileIndex.getValue(path)
            appendLine(
                "  <button class=\"file-btn\" type=\"button\" data-file=\"f$idx\" " +
                    "data-search=\"${HtmlEscape.escape(path.lowercase())}\" title=\"${HtmlEscape.escape(path)}\">" +
                    "<span class=\"file-name\">${HtmlEscape.escape(File(path).name)}</span>" +
                    "<span class=\"badge\">${entries.size}</span></button>",
            )
        }
        appendLine("</aside>")
        appendLine("<section class=\"detail\">")
        appendLine("  <div class=\"toolbar\">")
        appendLine("    <button id=\"prevBtn\" class=\"nav-btn\" type=\"button\" aria-label=\"Previous smell\">◀ Prev</button>")
        appendLine("    <span id=\"smellPos\" class=\"pos\">—</span>")
        appendLine("    <button id=\"nextBtn\" class=\"nav-btn\" type=\"button\" aria-label=\"Next smell\">Next ▶</button>")
        appendLine("    <span class=\"muted hint\">j/k or ↑/↓ to move · / to search</span>")
        appendLine("  </div>")
        appendLine(activeSmellPanel())
        byFile.forEach { (path, entries) ->
            val idx = fileIndex.getValue(path)
            appendLine(renderCodePanel(idx, path, sources[path], entries))
        }
        appendLine("</section>")
        appendLine("</div>")
    }

    private fun activeSmellPanel(): String =
        buildString {
            appendLine("  <div class=\"active-smell\" id=\"activeSmell\">")
            appendLine("    <div class=\"as-head\">")
            appendLine("      <span class=\"badge\" id=\"asRule\">—</span>")
            appendLine("      <span class=\"sev-badge\" id=\"asSev\"></span>")
            appendLine("      <span class=\"loc\" id=\"asLoc\"></span>")
            appendLine("    </div>")
            appendLine("    <p class=\"message\" id=\"asMsg\"></p>")
            appendLine("    <p class=\"suggestion\" id=\"asSug\" style=\"display:none\"></p>")
            appendLine("  </div>")
        }

    private fun renderCodePanel(
        idx: Int,
        path: String,
        source: List<String>?,
        entries: List<Pair<Int, Diagnostic>>,
    ): String {
        val hitLines = entries.flatMap { (_, d) -> d.location.line..d.location.endLine }.toSet()

        return buildString {
            appendLine("  <article class=\"file-panel\" data-file=\"f$idx\" id=\"file-f$idx\">")
            append("    <div class=\"panel-head\"><span class=\"path\">${HtmlEscape.escape(path)}</span>")
            if (source == null) {
                val note = if (sourceMode == SourceMode.SNIPPET) "snippet only" else "source unavailable — showing snippets"
                append("<span class=\"note\">$note</span>")
            }
            appendLine("</div>")

            // Per-file smell list: precise jumping inside large files.
            appendLine("    <ul class=\"smell-list\">")
            entries.forEach { (globalIndex, d) ->
                val lineText =
                    if (d.location.line == d.location.endLine) {
                        "L${d.location.line}"
                    } else {
                        "L${d.location.line}-${d.location.endLine}"
                    }
                appendLine(
                    "      <li><button class=\"smell-link\" type=\"button\" data-idx=\"$globalIndex\" " +
                        "data-file=\"f$idx\" data-rule=\"${HtmlEscape.escape(d.ruleId.name)}\" " +
                        "data-severity=\"${HtmlEscape.escape(d.severity.label)}\">" +
                        "<span class=\"dot sev-${HtmlEscape.escape(d.severity.label)}\"></span>" +
                        "<span class=\"sl-rule\">${HtmlEscape.escape(d.ruleName)}</span>" +
                        "<span class=\"sl-line\">$lineText</span></button></li>",
                )
            }
            appendLine("    </ul>")

            appendLine("    <div class=\"code-wrap\">")
            appendLine("      <pre class=\"code\"><code>")

            if (source == null) {
                entries.forEach { (_, diag) ->
                    val highlighted = KotlinHighlighter.highlight(snippetLines(diag))
                    appendLines(highlighted, diag.location.line, hitLines)
                }
            } else {
                val highlighted = KotlinHighlighter.highlight(source)
                val ranges =
                    if (source.size <= maxFileLines) {
                        listOf(1..source.size)
                    } else {
                        mergeWindows(entries.map { it.second }, source.size)
                    }
                var previousEnd = 0
                ranges.forEach { range ->
                    if (range.first > previousEnd + 1) {
                        val omitted = range.first - previousEnd - 1
                        appendLine("        <span class=\"omitted\">… $omitted lines omitted …</span>")
                    }
                    appendLines(highlighted.subList(range.first - 1, range.last), range.first, hitLines, range.last)
                    previousEnd = range.last
                }
            }

            appendLine("      </code></pre>")
            appendLine("    </div>")
            appendLine("  </article>")
        }
    }

    private fun StringBuilder.appendLines(
        highlighted: List<String>,
        firstLineNumber: Int,
        hitLines: Set<Int>,
        lastLineNumber: Int = firstLineNumber + highlighted.size - 1,
    ) {
        highlighted.forEachIndexed { i, html ->
            val number = firstLineNumber + i
            if (number > lastLineNumber) return@forEachIndexed
            val hit = if (number in hitLines) " hit" else ""
            append("        <span class=\"line$hit\" data-line=\"$number\">")
            append("<span class=\"ln\">$number</span>")
            append("<span class=\"lc\">$html</span>")
            appendLine("</span>")
        }
    }

    private fun snippetLines(diagnostic: Diagnostic): List<String> =
        diagnostic.codeSnippet
            .replace("\r\n", "\n")
            .trimEnd('\n')
            .split("\n")

    private fun mergeWindows(
        diagnostics: List<Diagnostic>,
        size: Int,
    ): List<IntRange> {
        val windows =
            diagnostics
                .map {
                    (it.location.line - CONTEXT_LINES).coerceAtLeast(1)..(it.location.endLine + CONTEXT_LINES).coerceAtMost(size)
                }.sortedBy { it.first }

        val merged = mutableListOf<IntRange>()
        windows.forEach { window ->
            val last = merged.lastOrNull()
            if (last == null || window.first > last.last + 1) {
                merged.add(window)
            } else {
                merged[merged.size - 1] = last.first..maxOf(last.last, window.last)
            }
        }
        return merged
    }

    private fun StringBuilder.cardsView(sorted: List<Diagnostic>) {
        appendLine("<div id=\"view-cards\" class=\"view is-hidden\">")
        appendLine("<section class=\"cards\">")
        sorted.forEach { diagnostic -> appendLine(card(diagnostic)) }
        appendLine("</section>")
        appendLine("</div>")
    }

    private fun smellEntries(
        sorted: List<Diagnostic>,
        fileIndex: Map<String, Int>,
    ): String =
        buildString {
            appendLine("<div id=\"smell-entries\" hidden>")
            sorted.forEachIndexed { index, d ->
                val searchText =
                    (
                        d.ruleName + " " + d.message + " " + d.location.filePath + " " +
                            d.description.orEmpty() + " " + d.codeSnippet
                    ).replace(Regex("\\s+"), " ").trim().lowercase()
                appendLine(
                    "  <div class=\"smell-entry\"" +
                        " data-idx=\"$index\"" +
                        " data-file=\"f${fileIndex.getValue(d.location.filePath)}\"" +
                        " data-rule=\"${HtmlEscape.escape(d.ruleId.name)}\"" +
                        " data-severity=\"${HtmlEscape.escape(d.severity.label)}\"" +
                        " data-start=\"${d.location.line}\"" +
                        " data-end=\"${d.location.endLine}\"" +
                        " data-line=\"${d.location.line}\"" +
                        " data-col=\"${d.location.column}\"" +
                        " data-filepath=\"${HtmlEscape.escape(d.location.filePath)}\"" +
                        " data-rule-name=\"${HtmlEscape.escape(d.ruleName)}\"" +
                        " data-message=\"${HtmlEscape.escape(d.message)}\"" +
                        " data-suggestion=\"${HtmlEscape.escape(d.suggestion.orEmpty())}\"" +
                        " data-search=\"${HtmlEscape.escape(searchText)}\"></div>",
                )
            }
            appendLine("</div>")
        }

    private fun StringBuilder.appendStat(
        value: Any,
        label: String,
    ) {
        appendLine("    <div class=\"stat\"><span class=\"stat-value\">$value</span><span class=\"stat-label\">$label</span></div>")
    }

    private fun card(diagnostic: Diagnostic): String {
        val searchText =
            (
                diagnostic.ruleName + " " +
                    diagnostic.message + " " +
                    diagnostic.location.filePath + " " +
                    diagnostic.description.orEmpty() + " " +
                    diagnostic.codeSnippet
            ).replace(Regex("\\s+"), " ").trim().lowercase()

        return buildString {
            append(
                "<article class=\"card sev-${diagnostic.severity.label}\"" +
                    " data-rule=\"${HtmlEscape.escape(diagnostic.ruleId.name)}\"" +
                    " data-severity=\"${HtmlEscape.escape(diagnostic.severity.label)}\"" +
                    " data-file=\"${HtmlEscape.escape(diagnostic.location.filePath)}\"" +
                    " data-search=\"${HtmlEscape.escape(searchText)}\">",
            )
            appendLine()
            appendLine("  <div class=\"card-head\">")
            appendLine("    <span class=\"badge\">${HtmlEscape.escape(diagnostic.ruleName)}</span>")
            appendLine("    <span class=\"sev-badge\">${HtmlEscape.escape(diagnostic.severity.label)}</span>")
            appendLine(
                "    <span class=\"loc\">${HtmlEscape.escape(diagnostic.location.filePath)}:" +
                    "${diagnostic.location.line}:${diagnostic.location.column}</span>",
            )
            appendLine("  </div>")
            appendLine("  <p class=\"message\">${HtmlEscape.escape(diagnostic.message)}</p>")
            appendLine(
                "  <pre class=\"snippet\"><code>${HtmlEscape.escape(diagnostic.codeSnippet.trim())}</code></pre>",
            )
            diagnostic.suggestion?.let {
                appendLine("  <p class=\"suggestion\">💡 ${HtmlEscape.escape(it)}</p>")
            }
            append("</article>")
        }
    }

    private fun usedRules(report: Report): List<Pair<String, String>> =
        report.smells
            .map { it.ruleId.name to it.ruleName }
            .distinct()
            .sortedBy { it.second }

    private fun severityRank(label: String): Int =
        when (label.lowercase()) {
            "high" -> 4
            "medium" -> 3
            "low" -> 2
            "info" -> 1
            else -> 0
        }

    private fun readLines(
        targetDir: File,
        path: String,
    ): List<String>? =
        try {
            val file = File(targetDir, path)
            if (!file.isFile) {
                null
            } else {
                val text = file.readText().replace("\r\n", "\n")
                val withoutTrailing = text.removeSuffix("\n")
                if (withoutTrailing.isEmpty()) listOf("") else withoutTrailing.split("\n")
            }
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val CONTEXT_LINES = 20
        const val DEFAULT_MAX_FILE_LINES = 2000

        val CSS =
            """
            :root {
              --bg: #0f1115; --panel: #171a21; --panel-2: #1f232c; --border: #2a2f3a;
              --text: #e6e8ec; --muted: #9aa4b2; --accent: #4f8cff;
              --high: #ef4444; --medium: #f59e0b; --low: #38bdf8; --info: #64748b;
            }
            * { box-sizing: border-box; }
            body { margin: 0; background: var(--bg); color: var(--text);
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
            .muted { color: var(--muted); }
            .is-hidden { display: none !important; }
            .topbar { display: flex; flex-wrap: wrap; gap: 1rem; align-items: center;
              justify-content: space-between; padding: 1.25rem 2rem; background: var(--panel);
              border-bottom: 1px solid var(--border); }
            .brand { display: flex; align-items: center; gap: 0.85rem; }
            .logo { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 10px;
              background: linear-gradient(135deg, var(--accent), #7c3aed); font-weight: 700; }
            h1 { margin: 0; font-size: 1.25rem; }
            .topbar p { margin: 0.15rem 0 0; font-size: 0.85rem; }
            .stats { display: flex; gap: 1.5rem; }
            .stat { display: flex; flex-direction: column; align-items: flex-end; }
            .stat-value { font-size: 1.35rem; font-weight: 700; }
            .stat-label { font-size: 0.75rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.06em; }
            main { max-width: 1400px; margin: 0 auto; padding: 1.5rem; }
            .chips { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1rem; }
            .chip { padding: 0.3rem 0.7rem; border-radius: 999px; background: var(--panel-2);
              border: 1px solid var(--border); font-size: 0.85rem; text-transform: capitalize; }
            .chip.sev-high { border-color: var(--high); } .chip.sev-medium { border-color: var(--medium); }
            .chip.sev-low { border-color: var(--low); } .chip.sev-info { border-color: var(--info); }
            .controls { display: flex; flex-wrap: wrap; gap: 0.75rem; align-items: center; margin-bottom: 1rem; }
            .controls input, .controls select { background: var(--panel); color: var(--text);
              border: 1px solid var(--border); border-radius: 8px; padding: 0.55rem 0.75rem; font-size: 0.9rem; }
            .controls input { flex: 1 1 280px; }
            .view-toggle { display: inline-flex; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }
            .tab { background: var(--panel); color: var(--muted); border: 0; padding: 0.55rem 0.9rem; cursor: pointer; font-size: 0.9rem; }
            .tab.active { background: var(--accent); color: #fff; }
            .empty-search { padding: 0.5rem 0.75rem; }
            #view-files { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 1rem; align-items: start; }
            .sidebar { position: sticky; top: 1rem; background: var(--panel); border: 1px solid var(--border);
              border-radius: 12px; padding: 0.75rem; max-height: calc(100vh - 2rem); overflow: auto; }
            .side-title { margin: 0.25rem 0.5rem 0.75rem; font-size: 0.75rem; text-transform: uppercase;
              letter-spacing: 0.06em; color: var(--muted); }
            .file-btn { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem;
              width: 100%; text-align: left; background: transparent; color: var(--text); border: 1px solid transparent;
              border-radius: 8px; padding: 0.5rem 0.6rem; cursor: pointer; font-size: 0.85rem; }
            .file-btn:hover { background: var(--panel-2); }
            .file-btn.active { background: var(--panel-2); border-color: var(--accent); }
            .file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
            .badge { background: var(--panel-2); border: 1px solid var(--border); border-radius: 6px;
              padding: 0.05rem 0.45rem; font-weight: 600; font-size: 0.78rem; }
            .detail { min-width: 0; }
            .toolbar { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
            .nav-btn { background: var(--panel); color: var(--text); border: 1px solid var(--border);
              border-radius: 8px; padding: 0.4rem 0.8rem; cursor: pointer; }
            .nav-btn:hover { border-color: var(--accent); }
            .pos { font-variant-numeric: tabular-nums; font-weight: 600; }
            .hint { font-size: 0.8rem; }
            .active-smell { background: var(--panel); border: 1px solid var(--border); border-left-width: 4px;
              border-left-color: var(--accent); border-radius: 12px; padding: 0.8rem 1rem; margin-bottom: 0.9rem; }
            .as-head { display: flex; flex-wrap: wrap; align-items: center; gap: 0.6rem; }
            .active-smell .message { margin: 0.55rem 0 0.15rem; }
            .active-smell .suggestion { margin: 0.35rem 0 0; color: #cbd5e1; font-size: 0.9rem; }
            .sev-badge { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--muted); }
            .loc { margin-left: auto; font-family: monospace; font-size: 0.8rem; color: var(--muted); }
            .file-panel { background: var(--panel); border: 1px solid var(--border); border-radius: 12px;
              margin-bottom: 1rem; overflow: hidden; display: none; }
            .file-panel.is-active { display: block; }
            .panel-head { padding: 0.6rem 0.9rem; border-bottom: 1px solid var(--border); font-size: 0.85rem;
              display: flex; justify-content: space-between; gap: 0.75rem; }
            .panel-head .path { font-family: monospace; }
            .panel-head .note { color: var(--medium); font-size: 0.78rem; }
            .smell-list { list-style: none; margin: 0; padding: 0.5rem 0.6rem; display: flex; flex-direction: column;
              gap: 0.2rem; max-height: 168px; overflow: auto; border-bottom: 1px solid var(--border); background: rgba(0,0,0,0.15); }
            .smell-link { display: flex; align-items: center; gap: 0.5rem; width: 100%; text-align: left;
              background: transparent; color: var(--text); border: 1px solid transparent; border-radius: 6px;
              padding: 0.3rem 0.5rem; cursor: pointer; font-size: 0.8rem; }
            .smell-link:hover { background: var(--panel-2); }
            .smell-link.active { background: var(--panel-2); border-color: var(--accent); }
            .smell-link .sl-rule { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
            .smell-link .sl-line { margin-left: auto; font-family: monospace; color: var(--muted); font-size: 0.75rem; }
            .dot { width: 9px; height: 9px; border-radius: 50%; flex: 0 0 auto; background: var(--muted); }
            .dot.sev-high { background: var(--high); } .dot.sev-medium { background: var(--medium); }
            .dot.sev-low { background: var(--low); } .dot.sev-info { background: var(--info); }
            .code-wrap { position: relative; max-height: 68vh; overflow: auto; background: #0b0d11; }
            .code { margin: 0; padding: 0.75rem 0; }
            .code code { font-family: "JetBrains Mono", Consolas, Monaco, monospace; font-size: 0.82rem; }
            .tok-comment { color: #8b949e; font-style: italic; }
            .tok-string { color: #a5d6ff; }
            .tok-keyword { color: #ff7b72; }
            .tok-number { color: #79c0ff; }
            .tok-annotation { color: #d2a8ff; }
            .tok-type { color: #ffa657; }
            .line { display: block; white-space: pre; padding: 0 0.75rem; }
            .line .ln { display: inline-block; width: 3.5em; text-align: right; color: var(--muted);
              user-select: none; padding-right: 1em; }
            .line.hit { box-shadow: inset 3px 0 0 var(--medium); }
            .line.hit-active { background: rgba(239, 68, 68, 0.22); box-shadow: inset 4px 0 0 var(--high); }
            .omitted { display: block; color: var(--muted); font-size: 0.78rem; padding: 0.2rem 0 0.2rem 4.5em; }
            .cards { display: block; }
            .card { background: var(--panel); border: 1px solid var(--border); border-left-width: 4px;
              border-radius: 12px; padding: 1rem 1.15rem; margin-bottom: 0.9rem; }
            .card.sev-high { border-left-color: var(--high); }
            .card.sev-medium { border-left-color: var(--medium); }
            .card.sev-low { border-left-color: var(--low); }
            .card.sev-info { border-left-color: var(--info); }
            .card-head { display: flex; flex-wrap: wrap; align-items: center; gap: 0.6rem; }
            .card .message { margin: 0.7rem 0 0.5rem; }
            .card .suggestion { margin: 0.5rem 0 0; color: #cbd5e1; font-size: 0.9rem; }
            .snippet { margin: 0; background: #0b0d11; border: 1px solid var(--border); border-radius: 8px;
              padding: 0.75rem; overflow-x: auto; }
            .snippet code { font-family: "JetBrains Mono", Consolas, Monaco, monospace; font-size: 0.85rem; white-space: pre; }
            .empty { text-align: center; padding: 4rem 1rem; color: #34d399; font-size: 1.2rem; font-weight: 600; }
            .failed { margin-top: 2rem; background: rgba(239, 68, 68, 0.06); border: 1px solid rgba(239, 68, 68, 0.3);
              border-radius: 12px; padding: 1rem 1.25rem; }
            .failed h2 { margin-top: 0; font-size: 1rem; color: var(--high); }
            .failed code { font-family: monospace; }
            .foot { margin-top: 2rem; font-size: 0.8rem; text-align: center; }
            @media (max-width: 860px) {
              #view-files { grid-template-columns: 1fr; }
              .sidebar { position: static; max-height: 240px; }
              .loc { margin-left: 0; width: 100%; }
            }
            """.trimIndent()

        val SCRIPT =
            """
            (function () {
              var entries = Array.prototype.slice.call(document.querySelectorAll('.smell-entry'));
              if (!entries.length) { return; }
              var cards = Array.prototype.slice.call(document.querySelectorAll('.card'));
              var fileBtns = Array.prototype.slice.call(document.querySelectorAll('.file-btn'));
              var panels = Array.prototype.slice.call(document.querySelectorAll('.file-panel'));
              var links = Array.prototype.slice.call(document.querySelectorAll('.smell-link'));
              var search = document.getElementById('search');
              var ruleSel = document.getElementById('ruleFilter');
              var sevSel = document.getElementById('severityFilter');
              var counter = document.getElementById('visibleCount');
              var noMatches = document.getElementById('noMatches');
              var posEl = document.getElementById('smellPos');
              var asRule = document.getElementById('asRule');
              var asSev = document.getElementById('asSev');
              var asLoc = document.getElementById('asLoc');
              var asMsg = document.getElementById('asMsg');
              var asSug = document.getElementById('asSug');
              var visible = entries.slice();
              var current = -1;

              function matchesAttr(el) {
                var q = (search.value || '').trim().toLowerCase();
                var rule = ruleSel.value;
                var sev = sevSel.value;
                if (q && (el.getAttribute('data-search') || '').indexOf(q) === -1) { return false; }
                if (rule && el.getAttribute('data-rule') !== rule) { return false; }
                if (sev && el.getAttribute('data-severity') !== sev) { return false; }
                return true;
              }

              function clearActiveLines() {
                Array.prototype.forEach.call(document.querySelectorAll('.line.hit-active'), function (l) {
                  l.classList.remove('hit-active');
                });
              }

              function showFile(panelId) {
                panels.forEach(function (p) { p.classList.toggle('is-active', p.getAttribute('data-file') === panelId); });
                fileBtns.forEach(function (b) { b.classList.toggle('active', b.getAttribute('data-file') === panelId); });
              }

              function markAndScroll(entry) {
                clearActiveLines();
                var panel = document.getElementById('file-' + entry.getAttribute('data-file'));
                if (!panel) { return; }
                var start = parseInt(entry.getAttribute('data-start'), 10);
                var end = parseInt(entry.getAttribute('data-end'), 10);
                var first = null;
                Array.prototype.forEach.call(panel.querySelectorAll('.line'), function (l) {
                  var n = parseInt(l.getAttribute('data-line'), 10);
                  if (n >= start && n <= end) { l.classList.add('hit-active'); if (!first) { first = l; } }
                });
                var wrap = panel.querySelector('.code-wrap');
                if (wrap && first) {
                  // Scroll inside the code container only, keeping the page (and toolbar) in place.
                  var top = first.offsetTop - (wrap.clientHeight / 2) + (first.clientHeight / 2);
                  wrap.scrollTop = Math.max(0, top);
                }
              }

              function updatePos() {
                if (posEl) { posEl.textContent = visible.length ? (current + 1) + '/' + visible.length : '0/0'; }
              }

              function markActiveLink(entry) {
                links.forEach(function (l) { l.classList.toggle('active', l.getAttribute('data-idx') === entry.getAttribute('data-idx')); });
              }

              function select(i) {
                if (!visible.length) { return; }
                if (i < 0) { i = 0; }
                if (i >= visible.length) { i = visible.length - 1; }
                current = i;
                var e = visible[i];
                showFile(e.getAttribute('data-file'));
                markAndScroll(e);
                markActiveLink(e);
                asRule.textContent = e.getAttribute('data-rule-name') || e.getAttribute('data-rule');
                asSev.textContent = e.getAttribute('data-severity');
                asLoc.textContent = e.getAttribute('data-filepath') + ':' + e.getAttribute('data-line') + ':' + e.getAttribute('data-col');
                asMsg.textContent = e.getAttribute('data-message') || '';
                var sug = e.getAttribute('data-suggestion') || '';
                asSug.textContent = sug ? '💡 ' + sug : '';
                asSug.style.display = sug ? '' : 'none';
                updatePos();
              }

              function applyFilter() {
                var previous = (current >= 0 && current < visible.length) ? visible[current] : null;
                visible = entries.filter(matchesAttr);
                entries.forEach(function (e) { e.classList.toggle('is-hidden', visible.indexOf(e) === -1); });
                cards.forEach(function (c) { c.classList.toggle('is-hidden', !matchesAttr(c)); });
                var visibleFiles = {};
                visible.forEach(function (e) { visibleFiles[e.getAttribute('data-file')] = true; });
                fileBtns.forEach(function (b) { b.classList.toggle('is-hidden', !visibleFiles[b.getAttribute('data-file')]); });
                links.forEach(function (l) {
                  var target = entries[parseInt(l.getAttribute('data-idx'), 10)];
                  l.classList.toggle('is-hidden', !target || visible.indexOf(target) === -1);
                });
                if (counter) { counter.textContent = String(visible.length); }
                if (noMatches) { noMatches.classList.toggle('is-hidden', visible.length !== 0); }
                if (!visible.length) {
                  clearActiveLines();
                  links.forEach(function (l) { l.classList.remove('active'); });
                  current = -1;
                  updatePos();
                  return;
                }
                var keep = (previous && visible.indexOf(previous) !== -1) ? visible.indexOf(previous) : 0;
                select(keep);
              }

              search.addEventListener('input', applyFilter);
              ruleSel.addEventListener('change', applyFilter);
              sevSel.addEventListener('change', applyFilter);

              document.getElementById('prevBtn').addEventListener('click', function () { select(current - 1); });
              document.getElementById('nextBtn').addEventListener('click', function () { select(current + 1); });

              fileBtns.forEach(function (b) {
                b.addEventListener('click', function () {
                  var f = b.getAttribute('data-file');
                  var first = null;
                  for (var i = 0; i < visible.length; i++) {
                    if (visible[i].getAttribute('data-file') === f) { first = i; break; }
                  }
                  if (first === null) { showFile(f); return; }
                  select(first);
                });
              });

              links.forEach(function (l) {
                l.addEventListener('click', function () {
                  var idx = parseInt(l.getAttribute('data-idx'), 10);
                  var e = entries[idx];
                  if (!e) { return; }
                  var vi = visible.indexOf(e);
                  if (vi === -1) { return; }
                  select(vi);
                });
              });

              var viewFilesBtn = document.getElementById('viewFilesBtn');
              var viewCardsBtn = document.getElementById('viewCardsBtn');
              var viewFiles = document.getElementById('view-files');
              var viewCards = document.getElementById('view-cards');
              function setView(v) {
                var files = v === 'files';
                viewFiles.classList.toggle('is-hidden', !files);
                viewCards.classList.toggle('is-hidden', files);
                viewFilesBtn.classList.toggle('active', files);
                viewCardsBtn.classList.toggle('active', !files);
              }
              viewFilesBtn.addEventListener('click', function () { setView('files'); });
              viewCardsBtn.addEventListener('click', function () { setView('cards'); });

              document.addEventListener('keydown', function (ev) {
                var tag = (ev.target && ev.target.tagName) || '';
                if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') { return; }
                if (ev.key === 'j' || ev.key === 'n' || ev.key === 'ArrowDown') { select(current + 1); ev.preventDefault(); }
                else if (ev.key === 'k' || ev.key === 'p' || ev.key === 'ArrowUp') { select(current - 1); ev.preventDefault(); }
                else if (ev.key === '/') { search.focus(); ev.preventDefault(); }
              });

              applyFilter();
              if (visible.length) { select(0); }
            })();
            """.trimIndent()
    }
}
