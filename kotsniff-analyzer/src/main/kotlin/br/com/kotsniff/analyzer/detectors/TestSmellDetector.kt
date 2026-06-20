package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.models.DetectionResult

interface TestSmellDetector {
    fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<DetectionResult>
}
