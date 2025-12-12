package shop.itbug.flutterx.manager

import com.google.common.base.CaseFormat
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartDefaultFormalNamedParameter
import com.jetbrains.lang.dart.psi.DartMetadata
import com.jetbrains.lang.dart.psi.DartNormalFormalParameter
import com.jetbrains.lang.dart.psi.impl.DartFieldFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartSimpleFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import shop.itbug.flutterx.document.getDartElementType
import shop.itbug.flutterx.util.DartPsiElementUtil


interface FieldManager {
    val myModel: MyDartFieldModel
}

data class FieldToFreezedConfig(
    var useDefault: Boolean = false,
    var useCamelCaseName: Boolean = false
)

///属性拼接
val MyDartFieldModel.fieldString: String
    get() {
        return "$typeString $fieldNameString"
    }

///是否为动态类型
val MyDartFieldModel.isDynamic
    get(): Boolean {
        return typeString == "dynamic"
    }

///获取参数的默认值字符串
val MyDartFieldModel.freezedDefaultText: String
    get() {
        if (isDynamic) {
            return ""
        }
        return when (val type = typeString) {
            "int" -> "0"
            "int?" -> "0"
            "String?" -> "\"\""
            "String" -> "\"\""
            "double?" -> "0.0"
            "double" -> "0.0"
            "num?" -> "0"
            "num" -> "0"
            "bool?" -> "false"
            "bool" -> "false"
            else -> {
                if (type.startsWith("List") || type.startsWith("List<") || type.startsWith("Set") || type.startsWith("Set<")) {
                    return "[]"
                } else if (type.startsWith("Map") || type.startsWith("Map<")) {
                    return "{}"
                } else if (type.startsWith("IList")) {
                    return "IListConst([])"
                } else if (type.startsWith("ISet")) {
                    return "ISetConst([])"
                } else if (type.startsWith("IMap")) {
                    return "IMapConst({})"
                }
                return ""
            }
        }
    }

///生成freezed code 例子 required String code or String? code
fun MyDartFieldModel.generateFreezedFieldCode(config: FieldToFreezedConfig = FieldToFreezedConfig()): String {
    val (useDefault, useCamelCaseName) = config
    val text: String
    var typeStr = typeString
    var requiredText = "required"
    if (useDefault) {
        requiredText = "@Default($freezedDefaultText)"
        if (isDynamic) {
            requiredText = ""
        }
    }


    var jsonKey = ""
    if (useCamelCaseName) {
        jsonKey = "@JsonKey(name:'${camelCaseName}')"
    }


    if (isOption || isDynamic) {
        if (useDefault) {
            typeStr = typeStr.removeSuffix("?")
            text = "$jsonKey $requiredText $typeStr $fieldNameString"
        } else {
            text = "$jsonKey $typeStr $fieldNameString"
        }
    } else {
        text = "$jsonKey $requiredText $typeStr $fieldNameString"
    }
    return text
}


///将驼峰变成下划线
private fun camelCaseToUnderscore(input: String): String {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, input)
}

val MyDartFieldModel.camelCaseName: String get() = camelCaseToUnderscore(fieldNameString)

/**
 * 封装为模型
 */
data class MyDartFieldModel(
    //类型字符串
    val typeString: String,
    //类型属性的名称
    val fieldNameString: String,
    //是否可以为null,true:可以为null
    val isOption: Boolean = false,
    val metadata: List<SmartPsiElementPointer<DartMetadata>>,
    val element: DartNormalFormalParameter
) {

    //默认值
    fun getDefaultValueString(): String? {
        val type = element.simpleFormalParameter?.type?.simpleType
        val typeManage = DartDefaultFormalNamedParameterActionManager.TypeManager(type)
        return typeManage.getMyDartType()?.defaultValueString
    }

    fun getMetaDataStrings(): String =
        if (metadata.isNotEmpty()) metadata.mapNotNull { it.element?.text }.joinToString("\n") else ""

    fun createFinalField(project: Project): DartVarDeclarationListImpl? {
        val ele: DartVarDeclarationListImpl? = DartPsiElementUtil.createDartVarDeclarationByText(
            project, """
${getMetaDataStrings()}
final $typeString $fieldNameString;
        """.trimIndent()
        )
        return ele
    }
}

/**
 * dart参数的处理
 */
val DartNormalFormalParameter.myManager get() = DartFieldsManager(this)

class DartFieldsManager(val element: DartNormalFormalParameter) : FieldManager {

    /**
     * 构建为我的模型
     */
    override val myModel: MyDartFieldModel
        get() {
            var typeString = ""
            var fieldNameString = ""
            val simpleParam = PsiTreeUtil.findChildOfType(element, DartSimpleFormalParameterImpl::class.java)
            var metadataList = listOf<SmartPsiElementPointer<DartMetadata>>()

            if (simpleParam != null) {
                simpleParam.type?.let {
                    typeString = it.text
                }
                fieldNameString = simpleParam.componentName.text
                metadataList = simpleParam.metadataList.map { metadata ->
                    return@map SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(metadata)
                }
            }
            val f = PsiTreeUtil.findChildOfType(element, DartFieldFormalParameterImpl::class.java)
            if (f != null) {
                typeString = f.referenceExpression.getDartElementType() ?: (f.type?.text ?: "-")
                fieldNameString = f.referenceExpression.text
                println("fieldNameString: $fieldNameString,typeString: $typeString")
                metadataList = f.metadataList.map { metadata ->
                    return@map SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(metadata)
                }
            }
            return MyDartFieldModel(
                typeString, fieldNameString, typeString.endsWith("?"), metadataList,
                element
            )
        }
}

val DartDefaultFormalNamedParameter.myManagerByNamed get() = DartNamedFieldsManager(this)

class DartNamedFieldsManager(val element: DartDefaultFormalNamedParameter) : FieldManager {
    override val myModel: MyDartFieldModel
        get() {
            return element.normalFormalParameter.myManager.myModel
        }

}