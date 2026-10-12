package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell
import br.com.kotsniff.core.models.Diagnostic

/**
 * A single test smell rule.
 *
 * Every rule exposes a stable [id] (used for configuration and reporting) and
 * produces normalised [Diagnostic]s.
 */
interface TestSmellDetector {
    /** The stable identifier of the rule implemented by this detector. */
    val id: TestSmell

    /**
     * Inspects a single test block and returns any detected smells.
     *
     * @param testBlock The test block to inspect.
     * @param filePath The file path used to build diagnostic locations.
     */
    fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic>
}
