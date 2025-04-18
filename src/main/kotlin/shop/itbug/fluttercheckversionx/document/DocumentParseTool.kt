package shop.itbug.fluttercheckversionx.document

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import org.dartlang.analysis.server.protocol.HoverInformation
import shop.itbug.fluttercheckversionx.manager.*
import shop.itbug.fluttercheckversionx.util.manager


interface GenerateDocumentTextImpl {
    fun generateDocument(): String
}

///对文档的类型显示进行了优化.
class DocumentParseTool(val element: PsiElement) {


    ///获取参数.
    fun getParams(): String? {
        return when (element) {

            is DartNamedConstructorDeclarationImpl -> {
                val c = element.getClassText
                return c
            }

            is DartMethodDeclarationImpl -> {
                return element.myManager.generateDocString()
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

fun generateClassByNames(
    className: String,
    componentNames: List<String>,
    requiredParams: List<MyDartFieldModel>,
    optionalParams: List<MyDartFieldModel>
): String {
    val sb = StringBuilder()
    sb.append(className)
    sb.append(" ")
    if (componentNames.size == 2) {
        sb.append("${componentNames[0]}.${componentNames[1]}")
    } else {
        if (componentNames.isNotEmpty()) {
            sb.append(componentNames.last())
        } else {
            sb.append(className)
        }

    }
    sb.append("(")


    //必填属性
    if (requiredParams.isNotEmpty()) {
        requiredParams.forEach {
            val fs = it.fieldString
            if (requiredParams.lastOrNull() == it && requiredParams.size > 1) {
                sb.appendLine("\t" + fs)
            } else {
                val lst = if (requiredParams.size == 1) "" else ","
                sb.appendLine("$fs$lst")
            }
        }
    }
    if (optionalParams.isNotEmpty()) {
        if (requiredParams.isNotEmpty()) {
            sb.append(",")
        }

        if (optionalParams.isNotEmpty()) {
            sb.appendLine("{")
        }

        optionalParams.forEach {
            val fs = it.fieldString
            if (optionalParams.lastOrNull() == it) {
                sb.appendLine("\t" + fs)
            } else {
                sb.appendLine("\t$fs,")
            }
        }
    }

    val lst = if (optionalParams.isNotEmpty()) "}" else ""

    //可选属性
    sb.append("$lst)")
    return sb.toString()
}


///组成一个类
val DartNamedConstructorDeclarationImpl.getClassText
    get() = generateClassByNames(
        this.myManager.getClassName,
        this.myManager.componentNameList,
        this.myManager.getRequiredFields,
        this.myManager.getNamedFields
    )


///处理构造函数的模型
class MyDartConstructorManager(private val dartConstructorPsiElement: DartFactoryConstructorDeclarationImpl) :
    GenerateDocumentTextImpl {

    private val manager: DartFactoryConstructorDeclarationImplManager = dartConstructorPsiElement.manager()

    private val className: String?
        get() {
            val dartClassPsi = PsiTreeUtil.findFirstParent(dartConstructorPsiElement) { it is DartClassDefinitionImpl }
            val name = dartClassPsi?.let { cs -> (cs as DartClassDefinitionImpl).componentName.name }
            return name
        }

    private val name get() = manager.getClassName


    override fun generateDocument(): String {
        val rf = manager.getRequiredFields
        val of = manager.getNamedFields
        val names = manager.componentNameList
        return generateClassByNames(className ?: name, names, rf, of)
    }


}


///DartClassDefinitionImpl 类型操作
///获取类型的定义
val DartClassDefinitionImpl.getDefinition: String
    get() {
        val bodyText = children.find { it is DartClassBodyImpl }?.text ?: ""
        val t = text.replace(bodyText, "")
        return t
    }


///获取dart类型
fun PsiElement.getDartElementType(): String? {
    return getDartInfo()?.staticType
}


///获取dart的信息(来自分析服务器)
fun PsiElement.getDartInfo(): HoverInformation? {
    val file = containingFile.virtualFile ?: return null
    val r =
        DartAnalysisServerService.getInstance(project).analysis_getHover(file, this.textRange.startOffset)
    if (r.isEmpty()) {
        return null
    }
    return r.first()
}

