package shop.itbug.fluttercheckversionx.document

import com.intellij.ide.projectView.ProjectView
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.annotations.Nls
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper
import java.io.File
import java.net.URI

/**
 * dart文件中对字符串进行本机文件解析.
 */
class DartImageDocumentProvider : AbstractDocumentationProvider(), ExternalDocumentationHandler {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): @Nls String? {
        element ?: return null
        originalElement ?: return null
        return DartPsiElementHelper.generateLocalImageDocument(element)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        contextElement ?: return null
        return DartPsiElementHelper.findTargetFilePsiElement(contextElement)
    }


    ///处理图片链接点击
    override fun handleExternalLink(psiManager: PsiManager?, link: String?, context: PsiElement?): Boolean {
        if (link != null && context != null && psiManager != null && link.startsWith("file:")) {
            try {
                val project = context.project
                val uri = URI.create(link)
                val file = runReadAction { LocalFileSystem.getInstance().findFileByIoFile(File(uri.path)) }
                if (file != null) {
                    ProjectView.getInstance(project).select(null, file, true) //文件浏览器中打开
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

}