package shop.itbug.fluttercheckversionx.manager

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.jetbrains.lang.dart.psi.*
import com.jetbrains.lang.dart.psi.impl.DartArgumentsImpl
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartMetadataImpl
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
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

/**
 * 构造函数的相关操作
 */
class DartFactoryConstructorDeclarationImplManager(private val psiElement: DartFactoryConstructorDeclarationImpl) {


    /**
     * 是否有参数
     */
    fun hasProperties(): Boolean {
        return psiElement.formalParameterList != null
    }


    /**
     * 获取类名
     */
    val getClassName get() : String? = psiElement.componentName?.text


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
            if ((manager.isOption || manager.firstIsRequiredTag) && !manager.hasMetadata("Default")) {


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


    ///参数是否标记有 required 标识
    val firstIsRequiredTag = finalElement?.firstChild?.text == "required"

    ///参数的类型
    val type = finalElement?.type?.simpleType

    ///是否为可选参数
    val isOption = type?.lastChild?.text == "?"

    ///类型管理
    val typeManager = TypeManager(type)

    ///字段名字
    private val filedName = finalElement?.componentName?.name ?: ""


    ///注解列表
    val getMetadataList: MutableCollection<DartMetadataImpl>
        get() = PsiTreeUtil.findChildrenOfType(
            finalElement,
            DartMetadataImpl::class.java
        )

    val getPropertiesWrapper
        get() = MyPropertiesWrapper(
            name = filedName,
            isRequired = !isOption,
            typeString = finalElement?.type?.simpleType?.text ?: "",
            jsonKeyName = getJsonKeyName ?: ""
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
                if (psiElementByNameKey?.isString == true) {
                    name = psiElementByNameKey.stringValue
                } else {
                    name = filedName
                }
            }
            return name
        }

    ///循环处理注解列表
    private fun processMetadata(
        filter: (manager: MyMetadataManger) -> Boolean,
        action: (manager: MyMetadataManger) -> Unit
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
        //是否必须的
        var isRequired: Boolean = false,
        // json字段属性
        var jsonKeyName: String = "",
        //类型
        var typeString: String = ""
    )

}


///几种常见的类型
enum class MyDartType(val dartType: String, val defaultValueString: String) {
    NumType("num", "0"), DoubleType("double", "0"), IntType("int", "0"), StringType("String", "''"), MapType(
        "Map",
        "{}"
    ),
    MapType2(
        "Map<String,dynamic>",
        "{}"
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

