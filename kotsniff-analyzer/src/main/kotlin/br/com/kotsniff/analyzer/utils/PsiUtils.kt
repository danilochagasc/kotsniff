package br.com.kotsniff.analyzer.utils

import br.com.kotsniff.core.models.CodeLocation
import org.jetbrains.kotlin.psi.KtElement

/**
 * Computes a [CodeLocation] for a PSI element.
 *
 * Line/column numbers are 1-based; [CodeLocation.endColumn] is exclusive.
 */
fun KtElement.toCodeLocation(filePath: String): CodeLocation {
    val fileText = this.containingFile.text
    val offset = this.textOffset

    val line = fileText.substring(0, offset).count { it == '\n' } + 1
    val lastNewLineIndex = fileText.lastIndexOf('\n', offset - 1)
    val column = offset - (if (lastNewLineIndex == -1) 0 else lastNewLineIndex + 1) + 1

    val endOffset = this.textRange.endOffset
    val endLine = fileText.substring(0, endOffset).count { it == '\n' } + 1
    val lastNewLineBeforeEnd = fileText.lastIndexOf('\n', endOffset - 1)
    val endColumn = endOffset - (if (lastNewLineBeforeEnd == -1) 0 else lastNewLineBeforeEnd + 1) + 1

    return CodeLocation(
        filePath = filePath,
        line = line,
        column = column,
        endLine = endLine,
        endColumn = endColumn,
    )
}
