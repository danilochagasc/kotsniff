package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.collectAssertionUnits
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.DUPLICATE_ASSERT
import br.com.kotsniff.core.models.Diagnostic

/**
 * Detects the exact same assertion repeated within one leaf test.
 *
 * Assertions are collected with the shared [collectAssertionUnits] policy, so
 * comparison stays strictly within a single leaf test (never across sibling
 * tests) and nested expressions are not counted twice.
 *
 * **Limitation:** comparison is on whitespace-normalised **source text**, so it
 * detects textual repetition only; it does not attempt type-dependent semantic
 * equivalence (e.g. swapped arguments), which would require resolution.
 */
class DuplicateAssertDetector : TestSmellDetector {
    override val id = DUPLICATE_ASSERT

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val seen = mutableSetOf<String>()
        val reported = mutableSetOf<String>()
        val results = mutableListOf<Diagnostic>()

        collectAssertionUnits(testBlock.body).forEach { unit ->
            val text = unit.text
            val normalized = text.replace(Regex("\\s+"), " ").trim()
            if (seen.contains(normalized)) {
                if (!reported.contains(normalized)) {
                    reported.add(normalized)
                    results.add(
                        Diagnostic.of(
                            DUPLICATE_ASSERT,
                            unit.toCodeLocation(filePath),
                            text,
                            message = "The same assertion is evaluated more than once in this test.",
                            suggestion = "Remove the redundant assertion or assert a distinct property.",
                        ),
                    )
                }
            } else {
                seen.add(normalized)
            }
        }

        return results
    }
}
