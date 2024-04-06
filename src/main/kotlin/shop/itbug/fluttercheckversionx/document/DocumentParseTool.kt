package shop.itbug.fluttercheckversionx.document

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import org.dartlang.analysis.server.protocol.HoverInformation
import shop.itbug.fluttercheckversionx.manager.document_type_string
import shop.itbug.fluttercheckversionx.manager.myManager
import shop.itbug.fluttercheckversionx.util.manager


interface GenerateDocumentTextImpl {
    fun generateDocument(): String
}

///对文档的类型显示进行了优化.
class DocumentParseTool(val element: PsiElement, val originalPsiElement: PsiElement) {


    ///获取参数.
    fun getParams(): String? {
        println(element::class.java)
        return when (element) {

            is DartNamedConstructorDeclarationImpl -> {
                element.getClassText
            }

            is DartMethodDeclarationImpl -> {
                return element.getDocumentText
            }

            is DartClassDefinitionImpl -> {
                return element.getDefinition
            }

            is DartFactoryConstructorDeclarationImpl -> {
                val manager = MyDartConstructorManager(element)
                return manager.generateDocument()
            }

            is DartVarAccessDeclarationImpl -> {
                val type = element.getDartElementType()
                val txt = element.text
                if (type != null && txt.contains(type)) {
                    return txt
                }
                return "${type ?: ""} ${element.text}"
            }

            is DartIdImpl -> {
                val type = element.getDartElementType()
                return "${type ?: ""} ${element.text}"
            }

            else -> {
                element.text
            }
        }
    }
}


private fun generateClassText(
    className: String, requiredParams: List<String>? = emptyList(), optionParams: List<String>? = emptyList()
): String {
    return """
class $className(
${
        if (!requiredParams.isNullOrEmpty()) requiredParams.joinToString(",", prefix = "".prependIndent("\t")) {
            it
        } else ""
    }${if (!requiredParams.isNullOrEmpty()) "," else ""}{
${
        optionParams?.joinToString(separator = ",\n") {
            it.prependIndent("\t")
        }
    }
})
    """.trimIndent()
}

///组成一个类
val DartNamedConstructorDeclarationImpl.getClassText
    get() = generateClassText(getNames, getRequiredParams, getOptionParams)

///必填参数
val DartNamedConstructorDeclarationImpl.getRequiredParams
    get() : List<String> {
        return formalParameterList.normalFormalParameterList.map { it.text }
    }

///可选参数
val DartNamedConstructorDeclarationImpl.getOptionParams
    get() : List<String> {
        return formalParameterList.optionalFormalParameters?.defaultFormalNamedParameterList?.map { it.myManager.getPropertiesWrapper.document_type_string }
            ?: emptyList()
    }

///函数名
val DartNamedConstructorDeclarationImpl.getNames
    get() : String {
        return componentNameList.joinToString(separator = ".") { it.text }
    }


///处理构造函数的模型
class MyDartConstructorManager(private val dartConstructorPsiElement: DartFactoryConstructorDeclarationImpl) :
    GenerateDocumentTextImpl {

    private val manager = dartConstructorPsiElement.manager()

    private val className: String?
        get() {
            val dartClassPsi =
                PsiTreeUtil.findFirstParent(dartConstructorPsiElement) { it is DartClassDefinitionImpl }
            val name = dartClassPsi?.let { cs -> (cs as DartClassDefinitionImpl).componentName.name }
            return name
        }

    private val name get() = dartConstructorPsiElement.componentName?.name

    val getFinalName = if (className == name) className else "$className.$name"
    override fun generateDocument(): String {

        val prop = manager.getPropertiesWrapper


        val sb = StringBuilder()
        prop.forEach {
            sb.appendLine(it.document_type_string.prependIndent("\t"))
        }


        return """
class $getFinalName(
{
$sb
})
""".trimIndent()
    }


}


///DartClassDefinitionImpl 类型操作
///获取类型的定义
val DartClassDefinitionImpl.getDefinition: String
    get() {
        val bodyText = children.find { it is DartClassBodyImpl }?.text ?: ""
        return text.replace(bodyText, "")
    }

///函数类型的定义
val DartMethodDeclarationImpl.getDocumentText: String
    get() {
        val bodyText = children.find { it is DartFunctionBodyImpl }?.text ?: ""
        return text.replace(bodyText, "")
    }


///获取dart类型
fun PsiElement.getDartElementType(): String? {
    return getDartInfo()?.staticType
}

/// 获取文档
fun PsiElement.getDartDocument(): String? {
    return getDartInfo()?.dartdoc
}


///获取dart的信息(来自分析服务器)
fun PsiElement.getDartInfo(): HoverInformation? {
    val r =
        DartAnalysisServerService.getInstance(project).analysis_getHover(containingFile.virtualFile, this.startOffset)
    if (r.isEmpty()) {
        return null
    }
    return r.first()
}

