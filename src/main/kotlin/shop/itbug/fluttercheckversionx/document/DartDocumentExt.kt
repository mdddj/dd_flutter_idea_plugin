package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.DartDefaultFormalNamedParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartFormalParameterListImpl
import com.jetbrains.lang.dart.psi.impl.DartNormalFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartOptionalFormalParametersImpl
import org.dartlang.analysis.server.protocol.HoverInformation

/**
 * dart 文件的文档注释扩展
 */
class DartDocumentExt : AbstractDocumentationProvider(), ExternalDocumentationProvider {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {


        if (element == null) return ""


        val reference = originalElement?.parent?.parent?.reference?.resolve()


        val result = DartAnalysisServerService.getInstance(element.project).analysis_getHover(
            element.containingFile.virtualFile,
            element.textOffset
        )
        if (result.isEmpty()) return ""
        val docInfo = result.first()
        val dartFormalParameterList = reference?.parent?.children?.filterIsInstance<DartFormalParameterListImpl>() ?: emptyList()
        return renderView(docInfo, element.project, if (dartFormalParameterList.isEmpty()) null else dartFormalParameterList.first())

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

        fun dartNormalElementHandle(
            element: DartNormalFormalParameterImpl,
            isRequired: Boolean,
            optional: Boolean
        ): DartParams {
            val or = element.children.first()
            val keyText = or.firstChild.text
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
                val dartRequiredParams = dartNormalElementHandle(it, true, false)
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
        sb.append("|  类型   | 名称  | 必填  | 位置 |")
        sb.append("\n")
        sb.append("|  ----  | ----  | ----  | ----  |")
        sb.append("\n")
        paramsArr.forEachIndexed { index, it ->
            sb.append("| ${it.key} | ${it.value} | ${it.isRequired} | ${if (it.optional) "{}" else "(必填参数${index+1})"} |")
            sb.append("\n")
        }
        return sb.toString()
    }

    ///渲染doc文档
    private fun renderView(info: HoverInformation, project: Project, referenceElement: PsiElement?): String {
        val sb = StringBuilder()

        sb.append(DocumentationMarkup.SECTIONS_START)
        if (info.containingClassDescription != null) {
            Helper.addKeyValueSection("类型", info.containingClassDescription, sb)
        }

        if (info.elementDescription != null) {
            Helper.addKeyValueSection("属性", Helper.markdown2Html(renderParams(referenceElement), project), sb)
        }

        if (info.dartdoc != null) {
            Helper.addKeyValueSection("文档", Helper.markdown2Html(info.dartdoc, project), sb)
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