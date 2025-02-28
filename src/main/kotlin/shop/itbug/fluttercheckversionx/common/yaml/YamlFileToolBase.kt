package shop.itbug.fluttercheckversionx.common.yaml

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLDocumentImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

abstract class YamlFileToolBase(val file: YAMLFile) {

    /**
     * 查找某个key下面的列表
     */
    suspend fun getChsWith(key: String) =
        getRootKeyValueList()?.find { it.keyText.trim() == key.trim() }?.findChild<YAMLBlockMappingImpl>()
            ?.chs<YAMLKeyValueImpl>() ?: emptyList()

    /**
     * *仅最顶部*
     * 搜索某个key的值
     * 例子: version: 1.0.0
     * 传参: version,返回:1.0.0
     */
    suspend fun searchValueWithRoot(key: String) =
        getRootKeyValueList()?.find { it.keyText.trim() == key.trim() }?.valueText?.trim()

    /**
     * 检查是否存在某个key
     */
    suspend fun hasKey(key: String) = getRootKeyValueList()?.find { it.keyText.trim() == key.trim() } != null
    suspend fun getYamlDocument() = file.findChild<YAMLDocumentImpl>()
    suspend fun getRootBlockMapping() = getYamlDocument()?.findChild<YAMLBlockMappingImpl>()
    suspend fun getRootKeyValueList() = getRootBlockMapping()?.chs<YAMLKeyValueImpl>()
}


suspend inline fun <reified T : PsiElement> PsiElement?.findChild() =
    readAction { PsiTreeUtil.getChildOfType(this, T::class.java) }

inline fun <reified T : PsiElement> PsiElement?.findChild2() =
    runReadAction { PsiTreeUtil.getChildOfType(this, T::class.java) }

suspend inline fun <reified T : PsiElement> PsiElement?.chs() =
    readAction { PsiTreeUtil.getChildrenOfType(this, T::class.java)?.toList()?.filterIsInstance<T>() } ?: emptyList()