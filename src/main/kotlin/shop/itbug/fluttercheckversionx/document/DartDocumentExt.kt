package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
/**
 * dart 文件的文档注释扩展
 */
class DartDocumentExt : AbstractDocumentationProvider(),ExternalDocumentationProvider {


    /**
     * 重构dart的文档,原生的太丑了
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {

        if(element == null) return ""
        val result = DartAnalysisServerService.getInstance(element.project).analysis_getHover(
            element.containingFile.virtualFile,
            element.textOffset
        )
        if(result.isEmpty()) return ""
        val docInfo =  result.first()
        val doc = docInfo.dartdoc // 获取到注释,然后再进行代码高亮
        if(doc != null){
            val renderText = MarkdownRender.renderText(doc, element.project)
            println(renderText)
            return renderText
        }
        return "空空如也";

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