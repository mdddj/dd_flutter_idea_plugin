package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import org.jetbrains.annotations.Nls
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper

/**
 * dart文件中对字符串进行本机文件解析.
 */
class DartImageDocumentProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): @Nls String? {
        element ?: return null
        return DartPsiElementHelper.generateLocalImageDocument(element)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        val e = contextElement.getTarget()
        return e
    }


    //获取目录元素
    private fun PsiElement?.getTarget(): PsiElement? {
        if (this == null) {
            return null
        }
        if (this is LeafPsiElement && this.parent is DartStringLiteralExpression) {
            return parent
        }
        return null
    }
}