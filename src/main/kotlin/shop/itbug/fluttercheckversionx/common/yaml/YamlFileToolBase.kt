package shop.itbug.fluttercheckversionx.common.yaml

import com.intellij.openapi.application.readAction
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
    suspend fun getChsWith(key: String): List<YAMLKeyValueImpl> {
        val keyValueList = getRootKeyValueList() ?: return emptyList()
        val find = keyValueList.find { it.keyText.trim() == key.trim() } ?: return emptyList()
        val r = find.findChild<YAMLBlockMappingImpl>() ?: return emptyList()
        return r.chs<YAMLKeyValueImpl>()
    }


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

suspend inline fun <reified T : PsiElement> PsiElement?.chs() =
    readAction { PsiTreeUtil.getChildrenOfType(this, T::class.java)?.toList()?.filterIsInstance<T>() } ?: emptyList()