package br.com.kotsniff.analyzer.corpus

import br.com.kotsniff.analyzer.AnalysisSettings
import br.com.kotsniff.analyzer.KotSniffEngine
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.Locale

private data class CorpusFile(
    val file: String,
    val reportDelay: Boolean,
    /** Expected findings as `RULE@line:column` (limitations excluded). */
    val expected: Set<String>,
    /** Documented limitations: asserted as regression guards, excluded from metrics. */
    val limitations: Set<String>,
)

private const val LIMITATION_MARK = "~"

/**
 * Runs the hand-annotated reference corpus and asserts every fixture against an
 * **occurrence-level** expectation (`RULE@line:column`), not just a count:
 *
 * - a missing finding, an unexpected finding, a wrong location or a duplicate
 *   finding all make the set comparison fail;
 * - a wrong rule identifier shows up as a set mismatch.
 *
 * The corpus is **synthetic**. Its metrics describe conformance to these
 * hand-written expectations; they are **not** evidence of real-world precision
 * or recall. Occurrences marked with `~` document knowingly unsupported
 * behaviour and are asserted as guards but excluded from the metrics.
 */
class CorpusRegressionTest :
    FunSpec({
        val corpus = loadManifest()
        val engines = mutableMapOf<Boolean, KotSniffEngine>()

        fun engineFor(reportDelay: Boolean): KotSniffEngine =
            engines.getOrPut(reportDelay) {
                KotSniffEngine.createDefault(AnalysisSettings(reportDelay = reportDelay))
            }

        fun actualOccurrences(
            file: CorpusFile,
            diagnostics: List<br.com.kotsniff.core.models.Diagnostic> =
                engineFor(file.reportDelay)
                    .analyze(readResource("corpus/${file.file}"), file.file),
        ): List<String> = diagnostics.map { "${it.ruleId.name}@${it.location.line}:${it.location.column}" }

        test("every fixture matches its expected occurrence set exactly") {
            corpus.forEach { file ->
                val actualList = actualOccurrences(file)
                val actualSet = actualList.toSet()
                val expectedAll = file.expected + file.limitations

                withClue("${file.file}: duplicated findings detected ($actualList)") {
                    actualList.size shouldBe actualSet.size
                }
                withClue("${file.file}: expected $expectedAll but got $actualSet") {
                    actualSet shouldBe expectedAll
                }
            }
        }

        test("occurrence-level corpus metrics are computed and non-regressed") {
            val metrics = linkedMapOf<String, Metrics>()
            corpus.forEach { file ->
                val actual = actualOccurrences(file).toSet() - file.limitations
                val expected = file.expected
                val rules = expected.map { ruleOf(it) }.toSet() + actual.map { ruleOf(it) }.toSet()
                rules.forEach { rule ->
                    val exp = expected.filter { ruleOf(it) == rule }.toSet()
                    val act = actual.filter { ruleOf(it) == rule }.toSet()
                    val metric = metrics.getOrPut(rule) { Metrics() }
                    metric.truePositives += (exp intersect act).size
                    metric.falsePositives += (act - exp).size
                    metric.falseNegatives += (exp - act).size
                }
            }

            val csv =
                buildString {
                    appendLine("rule,truePositives,falsePositives,falseNegatives,precision,recall,f1")
                    metrics.forEach { (rule, m) ->
                        appendLine(
                            "$rule,${m.truePositives},${m.falsePositives},${m.falseNegatives}," +
                                "${fmt(m.precision)},${fmt(m.recall)},${fmt(m.f1)}",
                        )
                    }
                }
            println("\nSynthetic corpus regression metrics (occurrence-level conformance):\n$csv")
            File("build/corpus-metrics.csv").writeText(csv)

            metrics.values.forEach { m ->
                m.falsePositives shouldBe 0
                m.falseNegatives shouldBe 0
            }
        }

        test("known limitations are explicitly documented") {
            val limitations = corpus.flatMap { file -> file.limitations.map { "${file.file}: $it" } }
            limitations.forEach { println("Known limitation: $it") }
            limitations.isNotEmpty() shouldBe true
        }
    })

private fun ruleOf(occurrence: String): String = occurrence.substringBefore('@')

private class Metrics {
    var truePositives = 0
    var falsePositives = 0
    var falseNegatives = 0

    // Zero-denominator convention: precision/recall are 1.0 when there is
    // nothing to predict/observe (documented in the README).
    val precision: Double
        get() = if (truePositives + falsePositives == 0) 1.0 else truePositives.toDouble() / (truePositives + falsePositives)

    val recall: Double
        get() = if (truePositives + falseNegatives == 0) 1.0 else truePositives.toDouble() / (truePositives + falseNegatives)

    val f1: Double
        get() = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
}

private fun fmt(value: Double): String = String.format(Locale.US, "%.3f", value)

private fun loadManifest(): List<CorpusFile> =
    readResource("corpus/manifest.csv")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            val parts = line.split("|")
            val occurrences =
                parts
                    .getOrNull(2)
                    .orEmpty()
                    .split(";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            CorpusFile(
                file = parts[0],
                reportDelay = parts.getOrNull(1)?.equals("true", ignoreCase = true) ?: false,
                expected = occurrences.filterNot { it.startsWith(LIMITATION_MARK) }.toSet(),
                limitations = occurrences.filter { it.startsWith(LIMITATION_MARK) }.map { it.removePrefix(LIMITATION_MARK) }.toSet(),
            )
        }.toList()

private fun readResource(path: String): String =
    CorpusRegressionTest::class.java.classLoader
        .getResource(path)
        ?.readText()
        ?: error("Missing test resource: $path")

private inline fun <T> withClue(
    clue: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: AssertionError) {
        throw AssertionError("$clue: ${e.message}", e)
    }
