package br.com.kotsniff.analyzer.detectors

import br.com.kotsniff.analyzer.utils.Assertions.calleeName
import br.com.kotsniff.analyzer.utils.toCodeLocation
import br.com.kotsniff.analyzer.visitors.KotestBlock
import br.com.kotsniff.core.enums.TestSmell.SLEEPY_TEST
import br.com.kotsniff.core.models.Diagnostic
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Detects tests that pause execution with explicit sleeps.
 *
 * - `Thread.sleep(...)` is always reported.
 * - `delay(...)` is reported only when [reportDelay] is enabled, because it is
 *   common in legitimate coroutine tests (default: disabled).
 */
class SleepyTestDetector(
    private val reportDelay: Boolean = false,
) : TestSmellDetector {
    override val id = SLEEPY_TEST

    override fun detect(
        testBlock: KotestBlock,
        filePath: String,
    ): List<Diagnostic> {
        if (!testBlock.isTerminal) return emptyList()

        val results = mutableListOf<Diagnostic>()

        testBlock.body.accept(
            object : KtTreeVisitorVoid() {
                override fun visitExpression(expression: KtExpression) {
                    super.visitExpression(expression)

                    when {
                        expression is KtCallExpression &&
                            reportDelay &&
                            expression.calleeName() == "delay" -> {
                            results.add(
                                Diagnostic.of(
                                    SLEEPY_TEST,
                                    expression.toCodeLocation(filePath),
                                    expression.text,
                                    message = "Test pauses with delay(...).",
                                    suggestion = "Avoid fixed delays; await the condition or use virtual time.",
                                ),
                            )
                        }

                        expression is KtDotQualifiedExpression &&
                            expression.text.startsWith("Thread.sleep") -> {
                            results.add(
                                Diagnostic.of(
                                    SLEEPY_TEST,
                                    expression.toCodeLocation(filePath),
                                    expression.text,
                                    message = "Test blocks the thread with Thread.sleep(...).",
                                    suggestion = "Avoid fixed sleeps; await the condition or use virtual time.",
                                ),
                            )
                        }
                    }
                }
            },
        )

        return results
    }
}
