package shop.itbug.fluttercheckversionx.manager

import com.jetbrains.lang.dart.psi.DartDefaultFormalNamedParameter
import com.jetbrains.lang.dart.psi.DartMetadata
import com.jetbrains.lang.dart.psi.DartNormalFormalParameter
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl

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

/**
 * 构造函数的相关操作
 */
class DartFactoryConstructorDeclarationImplManager(val psiElement: DartFactoryConstructorDeclarationImpl) {


    /**
     * 是否有参数
     */
    fun hasProperties(): Boolean {
        return psiElement.formalParameterList != null
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
        val defaultFormalParameters =
            psiElement.formalParameterList?.optionalFormalParameters?.defaultFormalNamedParameterList ?: emptyList()

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

        defaultFormalParameters.forEach {
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
}