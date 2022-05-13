package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartFile
import java.util.function.Consumer

/**
 * dart 文件的文档注释扩展
 */
class DartDocumentExt : AbstractDocumentationProvider(),ExternalDocumentationProvider {


    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
        if(file !is DartFile) return
        PsiTreeUtil.processElements(file) {
            println(it)
            true
        }
    }



    @Deprecated("Deprecated in Java", ReplaceWith(
        "CompositeDocumentationProvider.hasUrlsFor(this, element, originalElement)",
        "com.intellij.lang.documentation.CompositeDocumentationProvider"
    )
    )
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        return CompositeDocumentationProvider.hasUrlsFor(this,element, originalElement)
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return false
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {
    }
}