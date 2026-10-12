package br.com.kotsniff.analyzer.corpus

import br.com.kotsniff.analyzer.AnalysisSettings
import br.com.kotsniff.analyzer.KotSniffEngine
import br.com.kotsniff.core.enums.TestSmell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.Locale

private data class CorpusCase(
    val file: String,
    val rule: String,
    val expected: Int,
    val reportDelay: Boolean,
    val limitation: String?,
)

/**
 * Runs the hand-annotated reference corpus and asserts every case.
 *
 * The corpus is deliberately small and synthetic: it measures **conformance to
 * documented expectations**, not precision/recall on real projects. Cases
 * flagged with a limitation describe behaviours that are knowingly unsupported
 * (e.g. assertions hidden in helper functions); they are asserted to guard
 * against change but excluded from the aggregate metrics.
 */
class CorpusRegressionTest :
    FunSpec({
        val cases = loadManifest()
        val engines = mutableMapOf<Boolean, KotSniffEngine>()

        fun engineFor(reportDelay: Boolean): KotSniffEngine =
            engines.getOrPut(reportDelay) {
                KotSniffEngine.createDefault(AnalysisSettings(reportDelay = reportDelay))
            }

        fun detectedCount(case: CorpusCase): Int {
            val rule = TestSmell.valueOf(case.rule)
            return engineFor(case.reportDelay)
                .analyze(readResource("corpus/${case.file}"), case.file)
                .count { it.ruleId == rule }
        }

        test("every corpus case matches its expected detection count") {
            cases.forEach { case ->
                val detected = detectedCount(case)
                withClue("${case.file} [${case.rule}] expected ${case.expected} but got $detected") {
                    detected shouldBe case.expected
                }
            }
        }

        test("aggregate metrics are computed and non-regressed") {
            val metrics = linkedMapOf<String, Metrics>()
            cases
                .filter { it.limitation == null }
                .forEach { case ->
                    val detected = detectedCount(case)
                    val metric = metrics.getOrPut(case.rule) { Metrics() }
                    metric.truePositives += minOf(detected, case.expected)
                    metric.falsePositives += maxOf(0, detected - case.expected)
                    metric.falseNegatives += maxOf(0, case.expected - detected)
                }

            val csv =
                buildString {
                    appendLine("rule,truePositives,falsePositives,falseNegatives,precision,recall,f1")
                    metrics.forEach { (rule, m) ->
                        appendLine(
                            "$rule,${m.truePositives},${m.falsePositives},${m.falseNegatives},${fmt(
                                m.precision,
                            )},${fmt(m.recall)},${fmt(m.f1)}",
                        )
                    }
                }
            println("\nCorpus metrics (conformance to hand-annotated expectations):\n$csv")
            File("build/corpus-metrics.csv").writeText(csv)

            // With exact-match expectations asserted above, no non-limitation case may drift.
            metrics.values.forEach { m ->
                m.falsePositives shouldBe 0
                m.falseNegatives shouldBe 0
            }
        }

        test("known limitations are explicitly documented") {
            val limitations = cases.filter { it.limitation != null }
            limitations.forEach { case ->
                println("Known limitation: ${case.file} [${case.rule}] -> ${case.limitation}")
            }
            // The corpus must always state at least the helper-assertion limitation.
            limitations.isNotEmpty() shouldBe true
        }
    })

private class Metrics {
    var truePositives = 0
    var falsePositives = 0
    var falseNegatives = 0

    val precision: Double
        get() = if (truePositives + falsePositives == 0) 1.0 else truePositives.toDouble() / (truePositives + falsePositives)

    val recall: Double
        get() = if (truePositives + falseNegatives == 0) 1.0 else truePositives.toDouble() / (truePositives + falseNegatives)

    val f1: Double
        get() = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
}

private fun fmt(value: Double): String = String.format(Locale.US, "%.3f", value)

private fun loadManifest(): List<CorpusCase> =
    readResource("corpus/manifest.csv")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            val parts = line.split("|")
            CorpusCase(
                file = parts[0],
                rule = parts[1],
                expected = parts[2].toInt(),
                reportDelay = parts.getOrNull(3)?.equals("true", ignoreCase = true) ?: false,
                limitation = parts.getOrNull(4)?.takeIf { it.isNotBlank() },
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
