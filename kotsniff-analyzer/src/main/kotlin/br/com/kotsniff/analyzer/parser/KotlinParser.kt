package br.com.kotsniff.analyzer.parser

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Parses raw Kotlin source code into PSI trees.
 *
 * Uses a lightweight [KtPsiFactory] backed by a minimal compiler environment.
 * Only performs syntactic parsing — no type resolution or semantic analysis.
 *
 * Access is synchronized because the underlying PSI factory is shared and PSI
 * is not documented as thread-safe; the engine may parse files from several
 * coroutines. Parsing itself is cheap compared to creating the (lazy) compiler
 * environment, so serializing it has negligible cost while removing a data race.
 */
object KotlinParser {
    private val environment: KotlinCoreEnvironment by lazy {
        KotlinCoreEnvironment.createForProduction(
            { },
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )
    }

    private val psiFactory: KtPsiFactory by lazy {
        KtPsiFactory(environment.project)
    }

    /**
     * Parses the given Kotlin [code] string into a [KtFile] AST node.
     *
     * @param code The raw Kotlin source code to parse.
     * @param fileName An optional file name for the resulting [KtFile].
     * @return The parsed [KtFile].
     */
    @Synchronized
    fun parse(
        code: String,
        fileName: String = "Test.kt",
    ): KtFile = psiFactory.createFile(fileName, code)

    /**
     * Returns `true` when the parsed [file] contains syntax errors.
     *
     * A malformed file is parsed leniently into a partial PSI tree marked with
     * [PsiErrorElement] nodes; detecting them lets the CLI report the file as
     * unanalyzable instead of silently producing unreliable diagnostics.
     */
    fun hasSyntaxErrors(file: KtFile): Boolean = file.collectDescendantsOfType<PsiErrorElement>().isNotEmpty()
}
