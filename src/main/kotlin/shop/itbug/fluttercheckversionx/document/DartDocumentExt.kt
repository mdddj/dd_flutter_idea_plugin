package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import org.dartlang.analysis.server.protocol.HoverInformation
import shop.itbug.fluttercheckversionx.document.MarkdownRender.Companion.appendTag
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * dart 文件的文档注释扩展
 */

class DartDocumentExt : AbstractDocumentationProvider(), ExternalDocumentationProvider {


    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String {



        val reference = element.parent?.parent?.reference?.resolve()
        val result = DartAnalysisServerService.getInstance(element.project).analysis_getHover(
            element.containingFile.virtualFile,
            element.textOffset
        )
        println("文档result是否为空:${result.isEmpty()}")
        if (result.isEmpty()) return ""
        val docInfo = result.first()
        val dartFormalParameterList =
            reference?.parent?.children?.filterIsInstance<DartFormalParameterListImpl>() ?: emptyList()
        return renderView(
            docInfo,
            element.project,
            if (dartFormalParameterList.isEmpty()) null else dartFormalParameterList.first(),
            element,
            originalElement
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

        val paramsArr = arrayListOf<DartParams>()


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
        Helper.addMarkdownTableHeader(PluginBundle.get("type"), PluginBundle.get("name"), PluginBundle.get("required"), PluginBundle.get("location"), sb = sb)
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
        element: PsiElement,
        originalElement: PsiElement?
    ): String {
        val sb = StringBuilder()

        if(element.parent.text.isNotEmpty()){
            val simpleText = "```dart\n" +
                    (element.parent.text ?: element.text) + "\n ```\n"
            Helper.addKeyValueHeader(sb)
            sb.appendTag(MyMarkdownDocRenderObject(text = simpleText,project=project),PluginBundle.get("element"))
            Helper.addKeyValueFoot(sb)
        }

        Helper.addKeyValueHeader(sb)
        if (info.staticType != null) {
            Helper.addKeyValueSection(PluginBundle.get("type"), info.staticType, sb)
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
            sb.appendTag(obj, PluginBundle.get("doc"))
        }

        Helper.addKeyValueFoot(sb)
        return sb.toString()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("true"))
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        return true
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return false
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {
    }
}