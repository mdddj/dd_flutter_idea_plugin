package shop.itbug.fluttercheckversionx.document

import com.intellij.ide.projectView.ProjectView
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import org.dartlang.analysis.server.protocol.HoverInformation
import shop.itbug.fluttercheckversionx.document.MarkdownRender.Companion.appendTag
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.io.File
import java.net.URI

/**
 * dart 文件的文档注释扩展
 */

class DartDocumentExt : AbstractDocumentationProvider(), ExternalDocumentationHandler {


    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {


        //判断是不是资产
        val strElement = DartPsiElementHelper.findTargetFilePsiElement(element)
        if (strElement != null) {
            val result = DartPsiElementHelper.generateLocalImageDocument(element)
            if (result != null) {
                return result
            }
        }

        //生成普通的文档

        val reference = element.parent?.parent?.reference?.resolve()
        val file = element.containingFile.virtualFile ?: return null
        val project = element.project
        val result = DartAnalysisServerService.getInstance(project).analysis_getHover(
            file,
            element.textOffset
        )
        if (result.isEmpty()) return "Document not found"
        val docInfo = result.first()
        println("kind类型:${docInfo.elementKind}")
        val dartFormalParameterList =
            reference?.parent?.children?.filterIsInstance<DartFormalParameterListImpl>() ?: emptyList()
        return renderView(
            docInfo,
            project,
            if (dartFormalParameterList.isEmpty()) null else dartFormalParameterList.first(),
            element
        )

    }

    data class DartParams(
        val key: String,
        val value: String,
        val isRequired: Boolean,
        val optional: Boolean // 可选
    )

    /**
     * 渲染参数列表
     * 返回markdown表格
     */
    private fun renderParams(element: PsiElement?): String {

        if (element == null) {
            return ""
        }


        ///处理节点,如果有引用的对象,需要去引用的对象里面获取属性名字
        fun dartNormalElementHandle(
            element: DartNormalFormalParameterImpl,
            isRequired: Boolean,
            optional: Boolean
        ): DartParams {
            val or = element.children.first()
            val keyText: String
            val childrens = or.children.filterIsInstance<DartReferenceExpressionImpl>()
            keyText = if (childrens.isNotEmpty()) {
                MyDartPsiElementUtil.getRefreshMethodName(childrens.first())
            } else {
                or.firstChild.text
            }

            val valueText = or.lastChild.text
            return DartParams(
                key = keyText,
                value = valueText,
                isRequired = isRequired,
                optional = optional
            )
        }

        ///获取可选参数map
        fun getOptElement(element: DartDefaultFormalNamedParameterImpl): DartParams {
            val normal = element.children.filterIsInstance<DartNormalFormalParameterImpl>() //如果不为空则是可选参数
            val isRequired = element.firstChild.text == "required"
            return dartNormalElementHandle(normal.first(), isRequired, true)
        }

        val paramsArr = mutableListOf<DartParams>()


        //获取必填参数节点,不是{}里面声明的, fun(String str){}
        val requirdParams = element.children.filterIsInstance<DartNormalFormalParameterImpl>()
        if (requirdParams.isNotEmpty()) {
            requirdParams.forEach {
                val dartRequiredParams = dartNormalElementHandle(it, isRequired = true, optional = false)
                paramsArr.add(dartRequiredParams)
            }
        }


        //可选参数节点 ,fun({String str})
        val paramEle = element.children.filterIsInstance<DartOptionalFormalParametersImpl>()
        if (paramEle.isNotEmpty()) {
            val childParams = paramEle.first().children.filterIsInstance<DartDefaultFormalNamedParameterImpl>()

            childParams.forEach {
                val param = getOptElement(it)
                paramsArr.add(param)
            }
        }
        if (paramsArr.isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        Helper.addMarkdownTableHeader(
            PluginBundle.get("type"),
            PluginBundle.get("name"),
            PluginBundle.get("required"),
            PluginBundle.get("location"),
            sb = sb
        )
        paramsArr.forEachIndexed { index, it ->
            sb.append("| ${it.key} | ${it.value} | ${it.isRequired} | ${if (it.optional) "{}" else "(必填参数${index + 1})"} |")
            sb.append("\n")
        }
        return sb.toString()
    }

    ///渲染doc文档
    private fun renderView(
        info: HoverInformation,
        project: Project,
        referenceElement: PsiElement?,
        element: PsiElement
    ): String {
        val sb = StringBuilder()

        if (element.parent.text.isNotEmpty()) {
            val documentParseTool = DocumentParseTool(element.parent)
            var eleText: String? = null
            runReadAction {
                val params = documentParseTool.getParams()
                eleText = params
            }
            if (eleText != null) {
                val simpleText = "```dart\n" +
                        (eleText) + "\n```\n"
                Helper.addKeyValueHeader(sb)
                sb.appendTag(
                    MyMarkdownDocRenderObject(text = simpleText, project = project),
                    project,
                    PluginBundle.get("element")
                )
            }
            Helper.addKeyValueFoot(sb)
        }

        Helper.addKeyValueHeader(sb)
        if (info.staticType != null) {
            val t = info.staticType
            val tsb = StringBuilder()
            //eg: <a href="psi_element://String">String</a>
//            DocumentationManagerUtil.createHyperlink(tsb, t, t, true)
            println("type: $tsb")
            Helper.addKeyValueSection(PluginBundle.get("type"), t, sb)
        }
        if (referenceElement != null) {
            Helper.addKeyValueSection(
                PluginBundle.get("attributes"),
                Helper.markdown2Html(MyMarkdownDocRenderObject(renderParams(referenceElement), project)),
                sb
            )
        }
        if (info.dartdoc != null) {
            val obj = MyMarkdownDocRenderObject(
                text = info.dartdoc,
                project = element.project
            )
            sb.appendTag(obj, project, PluginBundle.get("doc"))
        }
        Helper.addKeyValueFoot(sb)

        val fullHtml = HtmlChunk.html().attr("width", "500px")
            .addRaw(sb.toString())
            .toString()
        println("\n$fullHtml\n")
        return fullHtml
    }

    override fun getDocumentationElementForLink(
        psiManager: PsiManager?,
        link: String?,
        context: PsiElement?
    ): PsiElement? {
        context ?: return null
        psiManager ?: return null
        link ?: return null
        return MyDartPsiElementUtil.searchClassByText(context.project, link)
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


    override fun handleExternalLink(psiManager: PsiManager?, link: String?, context: PsiElement?): Boolean {
        println("handleExternalLink: $link  $context")
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
        return super.handleExternalLink(psiManager, link, context)
    }

    override fun fetchExternalDocumentation(link: String, element: PsiElement?): String {
        println("fetchExternalDocumentation($link)  $element")
        return super.fetchExternalDocumentation(link, element)
    }


    override fun extractRefFromLink(link: String): String? {
        println("extractRefFromLink($link)")
        return super.extractRefFromLink(link)
    }


    override fun handleExternal(element: PsiElement?, originalElement: PsiElement?): Boolean {
        println("handleExternal($element)  $originalElement")
        return super.handleExternal(element, originalElement)
    }

    override fun canHandleExternal(element: PsiElement?, originalElement: PsiElement?): Boolean {
        println("canHandleExternal($element)  $originalElement")
        return super.canHandleExternal(element, originalElement)
    }

    override fun canFetchDocumentationLink(link: String?): Boolean {
        println("canFetchDocumentationLink($link)")
        return super.canFetchDocumentationLink(link)
    }
}