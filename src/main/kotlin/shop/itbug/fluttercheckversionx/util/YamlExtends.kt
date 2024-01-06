package shop.itbug.fluttercheckversionx.util

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl


private val devPattern = Regex("""\bdev\b""")
private val betaPattern = Regex("""\bbeta\b""")


enum class DartVersionType {
    Dev, Beta, Base
}


///插件版本是不是 dev
fun DartPluginVersionName.isDev(): Boolean = devPattern.containsMatchIn(version)
fun DartPluginVersionName.isBeta(): Boolean = betaPattern.containsMatchIn(version)

///版本类型
val DartPluginVersionName.versionType: DartVersionType
    get() = when {
        isBeta() -> DartVersionType.Beta
        isDev() -> DartVersionType.Dev
        else -> DartVersionType.Base
    }
val DartPluginVersionName.finalVersionText get() = version.removePrefix("^")

class DartPluginVersionName(val name: String, val version: String) {
    override fun toString(): String {
        return "插件名:$name,版本:$version,插件类型:$versionType"
    }
}


/**
 * yaml工具类
 */
class YamlExtends(val element: PsiElement) {

    ///判断是不是dart plugin 节点
    fun isDartPluginElement(): Boolean {
        if (element is YAMLKeyValueImpl && element.parent is YAMLBlockMappingImpl && element.parent.parent is YAMLKeyValueImpl && PsiTreeUtil.findChildOfType(
                element, YAMLBlockMappingImpl::class.java
            ) == null
        ) {
            val root = element.parent.parent as YAMLKeyValueImpl
            if (root.firstChild is LeafPsiElement) {
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