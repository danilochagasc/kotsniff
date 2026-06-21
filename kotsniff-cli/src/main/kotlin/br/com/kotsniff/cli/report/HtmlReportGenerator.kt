package br.com.kotsniff.cli.report

import br.com.kotsniff.cli.models.Report
import java.io.File

class HtmlReportGenerator : ReportGenerator {
    override fun generate(
        report: Report,
        outputFile: File,
        targetDir: File,
    ) {
        val groupedSmells = report.smells.groupBy { it.location.filePath }

        val html =
            buildString {
                appendLine("<!DOCTYPE html>")
                appendLine("<html lang=\"en\">")
                appendLine("<head>")
                appendLine("    <meta charset=\"UTF-8\">")
                appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                appendLine("    <title>KotSniff Analysis Report</title>")
                appendLine("    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap\" rel=\"stylesheet\">")
                appendLine("    <!-- Prism CSS (Okaidia dark theme fits well) -->")
                appendLine("    <link href=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-okaidia.min.css\" rel=\"stylesheet\" />")
                appendLine("    <link href=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-highlight/prism-line-highlight.min.css\" rel=\"stylesheet\" />")
                appendLine("    <style>")
                appendLine("        :root {")
                appendLine("            --bg-gradient: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);")
                appendLine("            --glass-bg: rgba(255, 255, 255, 0.05);")
                appendLine("            --glass-border: rgba(255, 255, 255, 0.1);")
                appendLine("            --primary: #818cf8;")
                appendLine("            --primary-glow: rgba(129, 140, 248, 0.4);")
                appendLine("            --text-main: #f8fafc;")
                appendLine("            --text-muted: #94a3b8;")
                appendLine("            --danger: #fb7185;")
                appendLine("            --warning: #fbbf24;")
                appendLine("            --success: #34d399;")
                appendLine("        }")
                appendLine("        body { font-family: 'Inter', sans-serif; margin: 0; padding: 0; display: flex; flex-direction: column; height: 100vh; background: var(--bg-gradient); color: var(--text-main); overflow: hidden; }")
                appendLine("        header { background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); padding: 1.5rem 3rem; border-bottom: 1px solid var(--glass-border); display: flex; justify-content: space-between; align-items: center; z-index: 10; box-shadow: 0 4px 30px rgba(0, 0, 0, 0.3); }")
                appendLine("        .brand { display: flex; align-items: center; gap: 1rem; }")
                appendLine("        h1 { margin: 0; font-size: 1.75rem; font-weight: 700; background: linear-gradient(to right, #818cf8, #c084fc); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: -0.5px; }")
                appendLine("        .summary-stats { display: flex; gap: 2.5rem; font-size: 0.95rem; font-weight: 600; color: var(--text-muted); }")
                appendLine("        .stat-value { color: var(--text-main); font-size: 1.1rem; margin-left: 0.4rem; }")
                appendLine("        .stat-success { color: var(--success); }")
                appendLine("        .stat-danger { color: var(--danger); }")
                appendLine("        .main-container { display: flex; flex: 1; overflow: hidden; padding: 1.5rem; gap: 1.5rem; position: relative; }")
                appendLine("        .sidebar { width: 320px; background: var(--glass-bg); backdrop-filter: blur(10px); border: 1px solid var(--glass-border); border-radius: 16px; overflow-y: auto; padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; box-shadow: 0 8px 32px rgba(0,0,0,0.2); }")
                appendLine("        .sidebar::-webkit-scrollbar { width: 6px; }")
                appendLine("        .sidebar::-webkit-scrollbar-thumb { background: var(--glass-border); border-radius: 3px; }")
                appendLine("        .content-area { flex: 1; overflow-y: auto; background: var(--glass-bg); backdrop-filter: blur(10px); border: 1px solid var(--glass-border); border-radius: 16px; padding: 2rem; box-shadow: 0 8px 32px rgba(0,0,0,0.2); position: relative; scroll-behavior: smooth; }")
                appendLine("        .content-area::-webkit-scrollbar { width: 8px; }")
                appendLine("        .content-area::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 4px; }")
                appendLine("        .file-btn { display: flex; align-items: center; gap: 0.75rem; width: 100%; text-align: left; padding: 1rem 1.25rem; background: rgba(255,255,255,0.03); border: 1px solid transparent; border-radius: 12px; cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); color: var(--text-muted); font-size: 0.9rem; font-weight: 500; font-family: 'Inter', sans-serif; position: relative; overflow: hidden; }")
                appendLine("        .file-btn:hover { background: rgba(255,255,255,0.08); transform: translateY(-2px); color: var(--text-main); }")
                appendLine("        .file-btn.active { background: rgba(129, 140, 248, 0.15); color: var(--primary); border-color: var(--primary-glow); box-shadow: 0 0 20px var(--primary-glow); }")
                appendLine("        .file-btn.active::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: var(--primary); border-radius: 4px 0 0 4px; }")
                appendLine("        .file-content { display: none; animation: fadeIn 0.4s ease-out forwards; }")
                appendLine("        .file-content.active { display: block; }")
                appendLine("        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }")
                appendLine("        .file-header { margin-top: 0; margin-bottom: 1.5rem; font-size: 1.3rem; font-weight: 600; color: var(--text-main); display: flex; align-items: center; gap: 0.75rem; padding-bottom: 1rem; border-bottom: 1px solid var(--glass-border); }")
                appendLine("        .smells-list { background: rgba(251, 191, 36, 0.1); border: 1px solid rgba(251, 191, 36, 0.2); color: #fde68a; padding: 1.25rem; border-radius: 12px; margin-bottom: 2rem; backdrop-filter: blur(4px); }")
                appendLine("        .smells-list ul { margin: 0.75rem 0 0 0; padding-left: 0; list-style: none; display: flex; flex-direction: column; gap: 0.5rem; }")
                appendLine("        .smell-item { display: flex; justify-content: space-between; align-items: center; padding: 0.5rem 1rem; background: rgba(0,0,0,0.15); border-radius: 8px; border: 1px solid rgba(255,255,255,0.05); }")
                appendLine("        .smells-list strong { color: var(--warning); }")
                appendLine("        .go-to-btn { background: rgba(251, 191, 36, 0.2); border: 1px solid rgba(251, 191, 36, 0.4); color: var(--warning); padding: 0.3rem 0.8rem; border-radius: 6px; cursor: pointer; font-size: 0.8rem; font-weight: 600; transition: all 0.2s; display: flex; align-items: center; gap: 0.4rem; }")
                appendLine("        .go-to-btn:hover { background: rgba(251, 191, 36, 0.4); color: white; transform: translateY(-1px); }")
                appendLine("        .floating-nav { position: fixed; bottom: 2rem; right: 3rem; display: flex; flex-direction: column; gap: 0.5rem; background: var(--glass-bg); backdrop-filter: blur(12px); padding: 0.5rem; border-radius: 12px; border: 1px solid var(--glass-border); box-shadow: 0 10px 40px rgba(0,0,0,0.4); z-index: 100; opacity: 0; pointer-events: none; transition: opacity 0.3s; }")
                appendLine("        .floating-nav.visible { opacity: 1; pointer-events: auto; }")
                appendLine("        .nav-btn { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: var(--text-main); width: 40px; height: 40px; border-radius: 8px; cursor: pointer; display: flex; justify-content: center; align-items: center; transition: all 0.2s; font-size: 1.2rem; }")
                appendLine("        .nav-btn:hover { background: rgba(129, 140, 248, 0.3); border-color: var(--primary); color: white; }")
                appendLine("        .failed-files { margin-top: 2rem; background: rgba(251, 113, 133, 0.05); border: 1px solid rgba(251, 113, 133, 0.2); padding: 1.25rem; border-radius: 12px; }")
                appendLine("        .failed-files h3 { color: var(--danger); margin-top: 0; font-size: 1rem; display: flex; align-items: center; gap: 0.5rem; }")
                appendLine("        .failed-file-item { color: #fecdd3; font-size: 0.85rem; padding: 0.4rem 0; border-bottom: 1px solid rgba(251, 113, 133, 0.1); }")
                appendLine("        .failed-file-item:last-child { border-bottom: none; }")
                appendLine("        pre[class*=\"language-\"] { margin: 0 !important; border-radius: 12px !important; background: rgba(0,0,0,0.3) !important; border: 1px solid var(--glass-border) !important; box-shadow: inset 0 2px 10px rgba(0,0,0,0.5); }")
                appendLine("        code[class*=\"language-\"] { font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace !important; font-size: 0.9rem !important; }")
                appendLine("        .line-highlight { background: rgba(251, 191, 36, 0.15) !important; border-left: 4px solid var(--warning) !important; box-shadow: inset 4px 0 10px rgba(251, 191, 36, 0.1); }")
                appendLine("    </style>")
                appendLine("</head>")
                appendLine("<body>")
                appendLine("    <header>")
                appendLine("        <div class=\"brand\">")
                appendLine("            <h1>KotSniff Report</h1>")
                appendLine("        </div>")
                appendLine("        <div class=\"summary-stats\">")
                appendLine("            <span>Files Scanned <span class=\"stat-value\">${report.totalFilesScanned}</span></span>")
                appendLine("            <span>Time <span class=\"stat-value\">${report.timeTakenMs}ms</span></span>")
                
                val smellColor = if (report.smellsFound == 0) "stat-success" else "stat-danger"
                appendLine("            <span>Total Smells <span class=\"stat-value $smellColor\">${report.smellsFound}</span></span>")
                appendLine("        </div>")
                appendLine("    </header>")
                appendLine("    <div class=\"main-container\">")

                // Sidebar
                appendLine("        <div class=\"sidebar\">")
                if (groupedSmells.isEmpty()) {
                    appendLine("            <div style=\"text-align: center; padding: 2rem 1rem;\">")
                    appendLine("                <div style=\"font-size: 3rem; margin-bottom: 1rem;\">🎉</div>")
                    appendLine("                <p style=\"color: var(--success); font-weight: 600; font-size: 1.1rem; margin: 0;\">Awesome!</p>")
                    appendLine("                <p style=\"color: var(--text-muted); font-size: 0.9rem; margin-top: 0.5rem;\">No test smells found.</p>")
                    appendLine("            </div>")
                } else {
                    appendLine("            <h3 style=\"margin-top: 0; margin-bottom: 1rem; color: var(--text-main); font-size: 0.95rem; text-transform: uppercase; letter-spacing: 1px; font-weight: 600;\">Files with Smells</h3>")
                    groupedSmells.keys.forEachIndexed { index, filePath ->
                        val activeClass = if (index == 0) "active" else ""
                        appendLine("            <button class=\"file-btn $activeClass\" onclick=\"showFile('$filePath')\" id=\"btn-$filePath\">")
                        appendLine("                📄 ${File(filePath).name}")
                        appendLine("            </button>")
                    }
                }

                if (report.failedFiles.isNotEmpty()) {
                    appendLine("            <div class=\"failed-files\">")
                    appendLine("                <h3>⚠️ Failed to Parse</h3>")
                    report.failedFiles.forEach { file ->
                        appendLine("                <div class=\"failed-file-item\">$file</div>")
                    }
                    appendLine("            </div>")
                }
                appendLine("        </div>")

                // Content Area
                appendLine("        <div class=\"content-area\" id=\"main-content-area\">")
                groupedSmells.forEach { (filePath, smells) ->
                    val isFirst = filePath == groupedSmells.keys.first()
                    val activeClass = if (isFirst) "active" else ""
                    val lineNumbers = smells.joinToString(",") { 
                        if (it.location.line == it.location.endLine) {
                            it.location.line.toString()
                        } else {
                            "${it.location.line}-${it.location.endLine}"
                        }
                    }

                    appendLine("            <div class=\"file-content $activeClass\" id=\"content-$filePath\">")
                    appendLine("                <h2 class=\"file-header\">📄 $filePath</h2>")
                    
                    // Smells summary
                    appendLine("                <div class=\"smells-list\">")
                    appendLine("                    <strong>Detected Smells:</strong>")
                    appendLine("                    <ul>")
                    smells.forEach { smell ->
                        val lineText = if (smell.location.line == smell.location.endLine) "line ${smell.location.line}" else "lines ${smell.location.line}-${smell.location.endLine}"
                        appendLine("                        <li class=\"smell-item\">")
                        appendLine("                            <span><strong>${smell.smellType.displayName}</strong> on $lineText</span>")
                        appendLine("                            <button class=\"go-to-btn\" onclick=\"scrollToSmell('${smell.location.line}')\">🎯 Go to smell</button>")
                        appendLine("                        </li>")
                    }
                    appendLine("                    </ul>")
                    appendLine("                </div>")

                    // Source code
                    val fileContent =
                        try {
                            File(targetDir, filePath).readText()
                        } catch (e: Exception) {
                            "// Could not read file content: ${e.message}"
                        }
                    
                    // Escape HTML in the source code
                    val escapedContent = fileContent.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                    appendLine("                <pre data-line=\"$lineNumbers\" style=\"white-space: pre-wrap;\"><code class=\"language-kotlin\">$escapedContent</code></pre>")
                    appendLine("            </div>")
                }

                if (groupedSmells.isEmpty()) {
                    appendLine("            <div style=\"display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: var(--text-muted);\">")
                    appendLine("                <div style=\"font-size: 4rem; margin-bottom: 1rem; opacity: 0.5;\">✨</div>")
                    appendLine("                <h2 style=\"margin: 0; color: var(--text-main); font-weight: 600;\">All Good!</h2>")
                    appendLine("                <p style=\"margin-top: 0.5rem;\">No test smells were detected in the scanned directory.</p>")
                    appendLine("            </div>")
                }

                appendLine("        </div>")
                
                // Floating Navigation
                if (groupedSmells.isNotEmpty()) {
                    appendLine("        <div class=\"floating-nav\" id=\"floating-nav\">")
                    appendLine("            <button class=\"nav-btn\" onclick=\"navigateSmell(-1)\" title=\"Previous Smell\">⬆️</button>")
                    appendLine("            <button class=\"nav-btn\" onclick=\"navigateSmell(1)\" title=\"Next Smell\">⬇️</button>")
                    appendLine("        </div>")
                }
                
                appendLine("    </div>")

                // Maps for JS
                appendLine("    <script>")
                appendLine("        const fileSmellsMap = {};")
                groupedSmells.forEach { (filePath, smells) ->
                    val linesArray = smells.map { it.location.line }.joinToString(",")
                    appendLine("        fileSmellsMap['$filePath'] = [$linesArray];")
                }
                appendLine("    </script>")

                // Prism Scripts
                appendLine("    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js\"></script>")
                appendLine("    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-kotlin.min.js\"></script>")
                appendLine("    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-highlight/prism-line-highlight.min.js\"></script>")

                // Interaction Logic
                appendLine("    <script>")
                appendLine("        let currentActiveFile = '${groupedSmells.keys.firstOrNull() ?: ""}';")
                appendLine("        let currentSmellIndex = -1;")
                appendLine("")
                appendLine("        function updateFloatingNav() {")
                appendLine("            const nav = document.getElementById('floating-nav');")
                appendLine("            if (!nav) return;")
                appendLine("            if (currentActiveFile && fileSmellsMap[currentActiveFile] && fileSmellsMap[currentActiveFile].length > 0) {")
                appendLine("                nav.classList.add('visible');")
                appendLine("            } else {")
                appendLine("                nav.classList.remove('visible');")
                appendLine("            }")
                appendLine("        }")
                appendLine("")
                appendLine("        function showFile(filePath) {")
                appendLine("            currentActiveFile = filePath;")
                appendLine("            currentSmellIndex = -1;")
                appendLine("            // Hide all contents")
                appendLine("            document.querySelectorAll('.file-content').forEach(el => {")
                appendLine("                el.classList.remove('active');")
                appendLine("            });")
                appendLine("            // Remove active state from all buttons")
                appendLine("            document.querySelectorAll('.file-btn').forEach(el => {")
                appendLine("                el.classList.remove('active');")
                appendLine("            });")
                appendLine("            // Show selected content")
                appendLine("            const contentDiv = document.getElementById('content-' + filePath);")
                appendLine("            if (contentDiv) {")
                appendLine("                contentDiv.classList.add('active');")
                appendLine("            }")
                appendLine("            // Activate selected button")
                appendLine("            const btn = document.getElementById('btn-' + filePath);")
                appendLine("            if (btn) {")
                appendLine("                btn.classList.add('active');")
                appendLine("            }")
                appendLine("            updateFloatingNav();")
                appendLine("            // Re-run Prism line-highlight to calculate correct positions after becoming visible")
                appendLine("            setTimeout(() => {")
                appendLine("                window.dispatchEvent(new Event('resize'));")
                appendLine("                // Reset scroll position to top when switching files")
                appendLine("                document.getElementById('main-content-area').scrollTop = 0;")
                appendLine("            }, 50);")
                appendLine("        }")
                appendLine("")
                appendLine("        function scrollToSmell(lineStart) {")
                appendLine("            if (!currentActiveFile) return;")
                appendLine("            // Update index tracking")
                appendLine("            const smells = fileSmellsMap[currentActiveFile];")
                appendLine("            if (smells) {")
                appendLine("                currentSmellIndex = smells.indexOf(parseInt(lineStart));")
                appendLine("            }")
                appendLine("            ")
                appendLine("            const container = document.getElementById('content-' + currentActiveFile);")
                appendLine("            if (!container) return;")
                appendLine("            // Wait for Prism to render the highlights if necessary")
                appendLine("            setTimeout(() => {")
                appendLine("                // Prism creates divs with data-start attribute for highlights")
                appendLine("                const highlight = container.querySelector(`.line-highlight[data-start=\"\${lineStart}\"]`);")
                appendLine("                if (highlight) {")
                appendLine("                    // Smooth scroll the highlight into view within the scrollable content-area")
                appendLine("                    highlight.scrollIntoView({ behavior: 'smooth', block: 'center' });")
                appendLine("                    // Add a brief pulse effect")
                appendLine("                    highlight.style.transition = 'background-color 0.3s';")
                appendLine("                    highlight.style.backgroundColor = 'rgba(251, 191, 36, 0.4)';")
                appendLine("                    setTimeout(() => { highlight.style.backgroundColor = ''; }, 600);")
                appendLine("                }")
                appendLine("            }, 100);")
                appendLine("        }")
                appendLine("")
                appendLine("        function navigateSmell(direction) {")
                appendLine("            if (!currentActiveFile) return;")
                appendLine("            const smells = fileSmellsMap[currentActiveFile];")
                appendLine("            if (!smells || smells.length === 0) return;")
                appendLine("            ")
                appendLine("            currentSmellIndex += direction;")
                appendLine("            if (currentSmellIndex < 0) currentSmellIndex = 0;")
                appendLine("            if (currentSmellIndex >= smells.length) currentSmellIndex = smells.length - 1;")
                appendLine("            ")
                appendLine("            scrollToSmell(smells[currentSmellIndex]);")
                appendLine("        }")
                appendLine("")
                appendLine("        // Initialize floating nav")
                appendLine("        updateFloatingNav();")
                appendLine("    </script>")
                appendLine("</body>")
                appendLine("</html>")
            }

        outputFile.writeText(html)
    }
}
