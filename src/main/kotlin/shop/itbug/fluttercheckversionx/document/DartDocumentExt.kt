package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import org.dartlang.analysis.server.protocol.HoverInformation

/**
 * dart 文件的文档注释扩展
 */
class DartDocumentExt : AbstractDocumentationProvider(), ExternalDocumentationProvider {


    /**
     * 重构dart的文档,原生的太丑了
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {

        if (element == null) return ""
        val result = DartAnalysisServerService.getInstance(element.project).analysis_getHover(
            element.containingFile.virtualFile,
            element.textOffset
        )
        if (result.isEmpty()) return ""
        val docInfo = result.first()
        return renderView(docInfo,element.project)

    }




    ///渲染doc文档
    private fun renderView(info: HoverInformation,project: Project): String {
        val sb = StringBuilder()

        sb.append(DocumentationMarkup.SECTIONS_START)
        if(info.containingClassDescription!=null){
            Helper.addKeyValueSection("类型", info.containingClassDescription, sb)
        }

        if(info.elementDescription!=null){
            Helper.addKeyValueSection("属性",info.elementDescription,sb)
        }

        if(info.dartdoc!=null){
            Helper.addKeyValueSection("文档",Helper.markdown2Html(info.dartdoc,project),sb)
        }
        sb.append(DocumentationMarkup.SECTIONS_END)
        return sb.toString()
    }

    @Deprecated(
        "Deprecated in Java", ReplaceWith(
            "CompositeDocumentationProvider.hasUrlsFor(this, element, originalElement)",
            "com.intellij.lang.documentation.CompositeDocumentationProvider"
        )
    )
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        return CompositeDocumentationProvider.hasUrlsFor(this, element, originalElement)
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return false
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {
    }
}