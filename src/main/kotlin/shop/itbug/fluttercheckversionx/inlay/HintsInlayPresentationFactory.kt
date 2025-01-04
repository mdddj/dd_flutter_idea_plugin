package shop.itbug.fluttercheckversionx.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement


fun Editor.getLine(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    return line
}

fun Editor.getLineStart(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    return document.getLineStartOffset(line)
}

/**
 * 获取缩进长度
 * 或者可以参考@[EditorUtil#getPlainSpaceWidth]
 */
fun Editor.getIndent(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    val lineStart = document.getLineStartOffset(line)
    return offset - lineStart
}


