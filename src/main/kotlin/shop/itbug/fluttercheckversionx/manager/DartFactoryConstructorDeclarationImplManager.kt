package shop.itbug.fluttercheckversionx.manager

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.DartDefaultFormalNamedParameter
import com.jetbrains.lang.dart.psi.DartSimpleFormalParameter
import com.jetbrains.lang.dart.psi.DartSimpleType
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.document.generateClassByNames
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil


sealed interface ReplaceElementActionResult
data object ReplaceElementIgnore : ReplaceElementActionResult //不做任何操作
data class ReplaceElementDoAction(val newElement: PsiElement) : ReplaceElementActionResult //替换节点


typealias ReplaceElementAction = (oldElement: DartDefaultFormalNamedParameter) -> ReplaceElementActionResult


///带有属性的基本封装类
interface DartFactoryConstructorDeclarationInterface {
    val componentNameList: List<String>
    val getRequiredFields: List<MyDartFieldModel>
    val getNamedFields: List<MyDartFieldModel>
    val getClassName: String
}

///生成doc
fun DartFactoryConstructorDeclarationInterface.generateDocString(): String {
    return generateClassByNames(this.getClassName, this.componentNameList, this.getRequiredFields, this.getNamedFields)
}

///全部的属性
val DartFactoryConstructorDeclarationInterface.allFieldList get() = getRequiredFields + getNamedFields


///生成freezed对象
/**
 * @freezed
 * class AppVersionInfo with _$AppVersionInfo {
 *   const factory AppVersionInfo({
 *       @JsonKey(name: 'id') @Default(0)  int id,
 *       @JsonKey(name: 'newVersion') @Default('')  String newversion,
 *       @JsonKey(name: 'apkUrl') @Default('')  String apkurl,
 *       @JsonKey(name: 'updateDescription') @Default('')  String updatedescription,
 *       @JsonKey(name: 'forceUpdate') @Default(0)  int forceupdate,
 *       @JsonKey(name: 'apkSize') @Default('')  String apksize,
 *       @JsonKey(name: 'uploadDate') @Default(0)  int uploaddate,
 *   }) = _AppVersionInfo;
 *
 *   factory AppVersionInfo.fromJson(Map<String, dynamic> json) => _$AppVersionInfoFromJson(json);
 * }
 *
 */
fun DartFactoryConstructorDeclarationInterface.generateFreezedClass(config: FieldToFreezedConfig = FieldToFreezedConfig()): String {
    val sb = StringBuilder()
    sb.appendLine("@freezed")
    sb.appendLine("class $getClassName with _$$getClassName {")
    sb.appendLine("\tconst factory $getClassName({")
    val all = allFieldList
    for (field in all) {
        val end = if (field == all.lastOrNull()) "" else ","
        sb.appendLine("\t\t${field.generateFreezedFieldCode(config)}$end")
    }
    sb.appendLine("\t}) = _$getClassName;")
    sb.appendLine("factory $getClassName.fromJson(Map<String, dynamic> json) => _$${getClassName}FromJson(json);")
    sb.appendLine("}")
    return "$sb"
}

/**
 * 构造函数的相关操作
 */
class DartFactoryConstructorDeclarationImplManager(private val psiElement: DartFactoryConstructorDeclarationImpl) :
    DartFactoryConstructorDeclarationInterface {


    ///名称列表
    override val componentNameList: List<String>
        get() {
            return psiElement.componentNameList.filter { it.name != null }.map { it.name ?: "" }
        }

    /**
     * 获取类名
     * 例子:
     * ```dart
     * factory Content.fromJson(Map<String, dynamic> json) => _$ContentFromJson(json);
     * ```
     * 返回:
     * ```bash
     * Content
     * ```
     */
    override val getClassName get() : String = psiElement.componentName?.text ?: ""


    /**
     * 获取参数列表
     */
    private val getFactoryParams
        get() = psiElement.formalParameterList?.optionalFormalParameters?.defaultFormalNamedParameterList ?: emptyList()


    /**
     * 获取参数列表
     */
    val getPropertiesWrapper: List<DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper>
        get() = getFactoryParams.map {
            DartDefaultFormalNamedParameterActionManager(
                it
            ).getPropertiesWrapper
        }


    /**
     * 获取固定位置的参数列表
     * 例子:
     * ```dart
     *  factory Content.fromJson(Map<String, dynamic> json) => _$ContentFromJson(json);
     * ```
     * 返回:
     * ```
     * Map<String, dynamic> json
     * ```
     */
    override val getRequiredFields: List<MyDartFieldModel>
        get() {
            val formalParameterList = psiElement.formalParameterList
            if (formalParameterList != null) {
                return formalParameterList.normalFormalParameterList.map { it.myManager.myModel }
            }
            return emptyList()
        }

    /**
     * 获取{}中的参数
     */
    override val getNamedFields: List<MyDartFieldModel>
        get() {
            val formalNamedParameter = psiElement.formalParameterList
            formalNamedParameter?.let {
                return formalNamedParameter.optionalFormalParameters?.defaultFormalNamedParameterList?.map { it.myManagerByNamed.myModel }
                    ?: emptyList()
            }
            return emptyList()
        }


    ///替换某个属性到新的节点
    private fun replaceOptionParameterTo(action: ReplaceElementAction) {
        getFactoryParams.let {
            val params = it
            params.forEach { oldParam ->
                when (val result = action(oldParam)) {
                    is ReplaceElementDoAction -> {
                        oldParam.replace(result.newElement)
                    }

                    ReplaceElementIgnore -> {
                    }
                }
            }
        }
    }


    ///重置为默认值,,,set as default value
    fun setAllPropertiesToDefaultValue() {
        replaceOptionParameterTo {
            val manager = DartDefaultFormalNamedParameterActionManager(it)
            if ((!manager.getPropertiesWrapper.isRequired || manager.firstIsRequiredTag) && !manager.hasMetadata("Default")) {


                manager.handleTypeElement { typePsiElement ->
                    typePsiElement?.let { _ ->
                        val type = manager.typeManager.getMyDartType()
                        type?.apply {
                            val genMetadata = MyDartPsiElementUtil.generateDartMetadata(
                                "Default(${this.defaultValueString})", psiElement.project
                            )

                            manager.findRequiredPsiElementAndAction { requiredElement ->
                                WriteCommandAction.runWriteCommandAction(it.project) {
                                    requiredElement.replace(genMetadata)
                                }
                            }


                        }

                    }
                }
            }
            ReplaceElementIgnore
        }
    }


}

///对属性的操作
class DartDefaultFormalNamedParameterActionManager(val element: DartDefaultFormalNamedParameter) {


    private val finalElement: DartSimpleFormalParameter? = element.normalFormalParameter.simpleFormalParameter

    private val thisParameterElement = element.normalFormalParameter.fieldFormalParameter


    ///参数是否标记有 required 标识
    val firstIsRequiredTag = finalElement?.firstChild?.text == "required"

    ///参数的类型
    val type = finalElement?.type?.simpleType


    ///类型管理
    val typeManager = TypeManager(type)

    ///字段名字
    private val filedName = finalElement?.componentName?.name ?: (thisParameterElement?.lastChild?.text ?: "")


    ///注解列表
    private val getMetadataList: MutableCollection<DartMetadataImpl>
        get() = PsiTreeUtil.findChildrenOfType(
            finalElement, DartMetadataImpl::class.java
        )

    val getPropertiesWrapper
        get() = MyPropertiesWrapper(
            name = filedName,
            typeString = finalElement?.type?.simpleType?.text ?: "",
            jsonKeyName = getJsonKeyName ?: "",
            element = element
        )


    ///查找 required element,并替换
    fun findRequiredPsiElementAndAction(handle: (ele: PsiElement) -> Unit) {
        val findChildrenOfType = PsiTreeUtil.findChildrenOfType(finalElement, LeafPsiElement::class.java)
        val find = findChildrenOfType.find { it.text == "required" }
        find?.let {
            handle.invoke(it)
        }
    }


    ///是否有[name]这个名字的注解,不带“@”符号
    fun hasMetadata(name: String): Boolean {
        val metadatas = finalElement?.childrenOfType<DartMetadataImpl>() ?: emptyList()
        return metadatas.find { it.firstChild.nextSibling.text == name } != null
    }


    ///操作属性名字节点
    fun handleTypeElement(handle: (ele: DartSimpleType?) -> Unit) {
        handle.invoke(type)
    }


    ///获取 json key 里面的 name 值
    private val getJsonKeyName
        get() : String? {
            var name: String? = null
            processMetadata({
                println(it.name)
                it.name == "JsonKey"
            }) {
                val psiElementByNameKey = it.getPsiElementByNameKey("name")
                name = if (psiElementByNameKey?.isString == true) {
                    psiElementByNameKey.stringValue
                } else {
                    filedName
                }
            }
            return name
        }

    ///循环处理注解列表
    private fun processMetadata(
        filter: (manager: MyMetadataManger) -> Boolean, action: (manager: MyMetadataManger) -> Unit
    ) {
        getMetadataList.forEach {
            val mg = MyMetadataManger(it)
            val isHandle = filter.invoke(mg)
            if (isHandle) {
                action.invoke(mg)
            }
        }
    }

    class TypeManager(dType: DartSimpleType?) {

        private val dartType = dType?.firstChild?.reference?.element?.text

        ///获取 dart 类型
        fun getMyDartType(): MyDartType? {
            return MyDartType.entries.find { it.dartType == dartType }
        }

    }

    class MyMetadataManger(metadata: DartMetadataImpl) {
        ///注解名
        val name: String = metadata.referenceExpression.text

        ///参数列表psi
        private val argPsiement = metadata.referenceExpression.nextSibling as DartArgumentsImpl

        ///参数列表
        private val args = argPsiement.argumentList?.namedArgumentList ?: emptyList()


        fun getPsiElementByNameKey(findName: String): MetaDataValueWrapper? {
            val findEle = args.find { it.parameterReferenceExpression.name == findName }
            if (findEle != null) {
                val isStringValue = findEle.lastChild is DartStringLiteralExpressionImpl
                return MetaDataValueWrapper(
                    isString = isStringValue,
                    stringValue = if (isStringValue) (findEle.lastChild as DartStringLiteralExpressionImpl).name else null
                )
            }
            return null
        }


    }

    data class MetaDataValueWrapper(val isString: Boolean, val stringValue: String?)


    data class MyPropertiesWrapper(
//        字段姓名
        var name: String = "",
        // json字段属性
        var jsonKeyName: String = "",
        //类型
        var typeString: String = "",
        //节点
        val element: DartDefaultFormalNamedParameter
    ) {
        fun getDefaultValue(): String {

            val type = element.normalFormalParameter.simpleFormalParameter?.type?.simpleType
                ?: element.normalFormalParameter.fieldFormalParameter?.type?.simpleType
            if (type == null) return "null"
            val typeManager = TypeManager(type)
            return typeManager.getMyDartType()?.defaultValueString ?: "null"
        }
    }

}

val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.isRequired get() = !final_type_string.endsWith("?")
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.type_string get() = "${typeString}${if (isRequired) "" else ""}"
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.constr_type_string get() = if (isRequired) "required this.${name}" else "this.${name}"
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.constr_type_string_use_default_value get() = if (isRequired) "this.${name}=${getDefaultValue()}" else "this.${name}"


val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.final_type_string: String
    get() {

        val f = element.firstChild?.firstChild
        val cs = PsiTreeUtil.findChildrenOfAnyType(f, LeafPsiElement::class.java)

        val t = cs.find {
            return@find it.text == "this" || it.text == "super"
        }
        if (t != null) {
            //处理this
            element.firstChild?.firstChild?.children?.find { it is DartReferenceExpressionImpl }?.let { ref ->
                run {
                    val d = DartAnalysisServerService.getInstance(ref.project)
                        .analysis_getHover(ref.containingFile.virtualFile, ref.textOffset)
                    if (d.isNotEmpty()) {
                        val type = d.first().staticType
                        return type
                    }
                }
            }
        }
        return typeString
    }

///几种常见的类型
enum class MyDartType(val dartType: String, val defaultValueString: String) {
    NumType("num", "0"), DoubleType("double", "0"), IntType("int", "0"), StringType("String", "''"), MapType(
        "Map", "{}"
    ),
    MapType2(
        "Map<String,dynamic>", "{}"
    ),
    ListType("List", "[]"), BoolType("bool", "false"), SetType("Set", "[]"), IListType("IList", "const IListConst([])"),

    IMapType("IMap", "const IMap({})"), ISetType("ISet", "const ISet([])")
}


