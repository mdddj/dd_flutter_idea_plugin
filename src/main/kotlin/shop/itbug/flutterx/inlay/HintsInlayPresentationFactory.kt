package shop.itbug.flutterx.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement


fun Editor.getLine(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    return line
}


