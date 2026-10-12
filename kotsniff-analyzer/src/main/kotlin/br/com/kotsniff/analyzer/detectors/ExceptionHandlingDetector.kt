package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.EXCEPTION_HANDLING
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression

/**
 * Detects `try/catch` blocks inside leaf tests.
 *
 * Catching exceptions in a test usually hides failures; the test framework
 * should be responsible for handling them (e.g. `assertFailsWith`).
 */
class ExceptionHandlingDetector : TestSmellDetector {
    override val id = EXCEPTION_HANDLING

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val results = mutableListOf<Diagnostic>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitTryExpression(expression: KtTryExpression) {
                    super.visitTryExpression(expression)

                    results.add(
                        Diagnostic.of(
                            EXCEPTION_HANDLING,
                            expression.toCodeLocation(filePath),
                            expression.text,
                            message = "Test handles exceptions with try/catch.",
                            suggestion = "Let the exception propagate or assert it with assertFailsWith/shouldThrow.",
                        ),
                    )
                }
            },
        )

        return results
    }
}
