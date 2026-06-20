package br.com.kotsniff.analyzer.parser

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Utility for parsing raw Kotlin source code into PSI trees.
 *
 * Uses a lightweight [KtPsiFactory] backed by a minimal compiler environment.
 * Only performs syntactic parsing — no type resolution or semantic analysis.
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
    fun parse(
        code: String,
        fileName: String = "Test.kt",
    ): KtFile = psiFactory.createFile(fileName, code)
}
