package shop.itbug.fluttercheckversionx.util

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl


data class DartPluginVersionName(val name: String,val version: String)
/**
 * yaml工具类
 */
class YamlExtends(val element: PsiElement) {

    ///判断是不是dart plugin 节点
     fun isDartPluginElement(): Boolean {
        if (element is YAMLKeyValueImpl && element.parent is YAMLBlockMappingImpl && element.parent.parent is YAMLKeyValueImpl && PsiTreeUtil.findChildOfType(element,YAMLBlockMappingImpl::class.java)==null) {
            val root = element.parent.parent as YAMLKeyValueImpl
            if (root.firstChild is LeafPsiElement ) {
                val temp = root.firstChild.text
                if (temp == "dependencies" || temp == "dependency_overrides" || temp == "dev_dependencies") {
                    return true
                }
            }
        }
        return false
    }

    ///获取依赖名字
    fun getDartPluginNameAndVersion(): DartPluginVersionName? {
        if (isDartPluginElement()) {
            val ele = (element as YAMLKeyValueImpl)
            return DartPluginVersionName(name = ele.keyText, version = ele.valueText)
        }
        return null
    }

}