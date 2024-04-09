package shop.itbug.fluttercheckversionx.manager

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.*
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.document.generateClassByNames
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil


/**
 * 封装自己的属性节点
 * @param option 是否在必填参数中,  true 表示可选则在 {} 中
 */
data class ConstructorProperties(
    val name: String,
    val index: Int,
    val dartDefaultFormalNamedParameter: DartDefaultFormalNamedParameter? = null,
    val dartNormalFormalParameter: DartNormalFormalParameter? = null,
    val dataMetadataList: List<DartMetadata> = emptyList(),
    val option: Boolean
)

sealed interface ReplaceElementActionResult
data object ReplaceElementIgnore : ReplaceElementActionResult //不做任何操作
data class ReplaceElementDoAction(val newElement: PsiElement) : ReplaceElementActionResult //替换节点


typealias ReplaceElementAction = (oldElement: DartDefaultFormalNamedParameter) -> ReplaceElementActionResult

fun DartFactoryConstructorDeclarationInterface.generateDocString(): String {
    return generateClassByNames(this.getClassName, this.componentNameList, this.getRequiredFields, this.getNamedFields)
}

interface DartFactoryConstructorDeclarationInterface {
    val componentNameList: List<String>
    val getRequiredFields: List<MyDartFieldModel>
    val getNamedFields: List<MyDartFieldModel>
    val getClassName: String
}

//扩展
val DartFactoryConstructorDeclarationImpl.myManager get() = DartFactoryConstructorDeclarationImplManager(this)

/**
 * 构造函数的相关操作
 */
class DartFactoryConstructorDeclarationImplManager(private val psiElement: DartFactoryConstructorDeclarationImpl) :
    DartFactoryConstructorDeclarationInterface {


    /**
     * 是否有参数
     */
    fun hasProperties(): Boolean {
        return psiElement.formalParameterList != null
    }


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
     * 是否有HiveType注解
     */
    fun hasHiveMate() = psiElement.getMetadataByName("HiveType") != null


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

    /**
     * 封装为自己的
     * 获取构造函数的所有参数
     */
    fun getAllProperties(): List<ConstructorProperties> {
        val result = mutableListOf<ConstructorProperties>()
        val normalParameters = psiElement.formalParameterList?.normalFormalParameterList ?: emptyList()

        normalParameters.forEachIndexed { index, it ->

            result.add(
                ConstructorProperties(
                    name = it.simpleFormalParameter?.componentName?.text ?: "",
                    index = index,
                    option = false,
                    dartNormalFormalParameter = it,
                    dataMetadataList = it.simpleFormalParameter?.metadataList ?: emptyList()
                )
            )
        }

        getFactoryParams.forEach {
            result.add(
                ConstructorProperties(
                    name = it.normalFormalParameter.simpleFormalParameter?.componentName?.text ?: "",
                    index = result.size + 1,
                    dartDefaultFormalNamedParameter = it,
                    option = true,
                    dataMetadataList = it.normalFormalParameter.simpleFormalParameter?.metadataList ?: emptyList()
                )
            )

        }

        return result
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

val DartDefaultFormalNamedParameter.myManager get() = DartDefaultFormalNamedParameterActionManager(this)

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


    ///创建一个注解
    fun createMetedata(name: String): DartMetadataImpl {
        return MyDartPsiElementUtil.generateDartMetadata(name, element.project)
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

    inner class TypeManager(dType: DartSimpleType?) {

        private val dartType = dType?.firstChild?.reference?.element?.text

        ///获取 dart 类型
        fun getMyDartType(): MyDartType? {
            return MyDartType.entries.find { it.dartType == dartType }
        }

    }

    inner class MyMetadataManger(metadata: DartMetadataImpl) {
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
    )

}

val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.isRequired get() = !final_type_string.endsWith("?")
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.type_string get() = "${typeString}${if (isRequired) "" else ""}"
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.constr_type_string get() = if (isRequired) "required this.${name}" else "this.${name}"

///是否有默认初始值,true -> 有
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.hasInitValue: Boolean
    get() {
        val cs = PsiTreeUtil.findChildrenOfAnyType(element, LeafPsiElement::class.java)
        val t = cs.find {
            return@find it.text == "="
        }
        return t != null
    }

///获取初始值的文本
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.get_init_text: String
    get() {
        return element.lastChild.text
    }
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
val DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper.document_type_string: String
    get() {
        val s = hasInitValue
        var t = if (isRequired) "${if (s) "" else "required "}$final_type_string $name" else "$final_type_string $name"
        if (s) {
            t = "$t = $get_init_text"
        }
        return t
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


///对注解的一些操作
class DartMetaActionManager(private val dartMetadata: DartMetadataImpl) {


    /**
     * 根据一个字符串来生成一个注解 PsiElement
     * 注意:[string] 不要携带@
     * 例子:
     * ```kotlin
     * @Default(0) -> string 传入`Default(0)`就行
     *
     * ```
     */
    fun generateForStringText(string: String): DartMetadataImpl {
        return MyDartPsiElementUtil.generateDartMetadata(string, dartMetadata.project)
    }
}

