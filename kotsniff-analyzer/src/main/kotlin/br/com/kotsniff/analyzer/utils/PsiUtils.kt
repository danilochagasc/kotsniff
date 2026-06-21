package br.com.kotsniff.analyzer.utils

import br.com.kotsniff.core.models.CodeLocation
import org.jetbrains.kotlin.psi.KtElement

fun KtElement.toCodeLocation(filePath: String): CodeLocation {
    val fileText = this.containingFile.text
    val offset = this.textOffset

    val line = fileText.substring(0, offset).count { it == '\n' } + 1
    val lastNewLineIndex = fileText.lastIndexOf('\n', offset - 1)
    val column = offset - (if (lastNewLineIndex == -1) 0 else lastNewLineIndex + 1) + 1

    val endOffset = this.textRange.endOffset
    val endLine = fileText.substring(0, endOffset).count { it == '\n' } + 1

    return CodeLocation(filePath, line, column, endLine)
}
