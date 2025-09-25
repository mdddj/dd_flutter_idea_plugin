package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.common.yaml.DartYamlModel
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.MyDartPackage
import shop.itbug.fluttercheckversionx.tools.YAML_DART_PACKAGE_INFO_KEY


private val devPattern = Regex("""\bdev\b""")
private val betaPattern = Regex("""\bbeta\b""")


enum class DartVersionType {
    Dev, Beta, Base, Any
}

/**
 * 使用正则表达式
 * 解析dart package版本类型
 */
fun tryParseDartVersionType(version: String): DartVersionType {
    if (devPattern.containsMatchIn(version)) return DartVersionType.Dev
    if (betaPattern.containsMatchIn(version)) return DartVersionType.Beta
    if (version.isEmpty() || version == "any") return DartVersionType.Any
    return DartVersionType.Base
}

///插件版本是不是 dev
fun DartPluginVersionName.isDev(): Boolean = devPattern.containsMatchIn(version)
fun DartPluginVersionName.isBeta(): Boolean = betaPattern.containsMatchIn(version)

///版本类型
val DartPluginVersionName.versionType: DartVersionType
    get() = tryParseDartVersionType(version)

val DartPluginVersionName.finalVersionText get() = version.removePrefix("^")

data class DartPluginVersionName(val name: String, val version: String)


/**
 * yaml工具类
 */
class YamlExtends(val element: PsiElement) {

    ///判断是不是dart plugin 节点
    fun isDartPluginElement(): Boolean {
        if (element !is YAMLKeyValueImpl) return false
        if (element.parent !is YAMLBlockMappingImpl) return false
        if (element.parent.parent !is YAMLKeyValueImpl) return false
        val igPluginNames = listOf("flutter_localizations", "flutter")
        val devTypes = listOf("dependencies", "dependency_overrides", "dev_dependencies")
        val rootElement: YAMLKeyValueImpl =
            PsiTreeUtil.getParentOfType(element, YAMLKeyValueImpl::class.java, true, 2) ?: return false
        if (devTypes.contains(rootElement.keyText) && !igPluginNames.contains(element.keyText)) return true
        return false
    }

    ///获取依赖名字
    fun getDartPluginNameAndVersion(): DartPluginVersionName? {
        if (isDartPluginElement()) {
            val ele = (element as YAMLKeyValueImpl)
            if (ele.valueText == "any") return null
            return DartPluginVersionName(name = ele.keyText, version = ele.valueText)
        }
        return null
    }


    /**
     * 是否有path, git 指定版本
     *  例子:
     *  ```dart
     *   vimeo_video_player:
     *     path: ../../hlx/github/vimeo_video_player
     *  ```
     *  @return true
     */
    private fun isSpecifyVersion(): Boolean =
        (element is YAMLKeyValueImpl) && PsiTreeUtil.findChildOfType(element, YAMLBlockMappingImpl::class.java) != null


    /**
     * 解析获取包模型
     */
    fun getMyDartPackageModel(): MyDartPackage? {
        val info: DartPluginVersionName = getDartPluginNameAndVersion() ?: return null
        if (!isSpecifyVersion()) {
            val lastTextElement = PsiTreeUtil.findChildOfType(element, YAMLPlainTextImpl::class.java)
            if (lastTextElement != null) {
                val item = MyDartPackage(
                    info.name, element as YAMLKeyValueImpl, info, lastTextElement
                )
                return item
            }
        }
        return null
    }

    fun tryGetModels(): List<DartYamlModel> {
        val file = element.containingFile?.virtualFile ?: return emptyList()
        val psiFile = PsiManager.getInstance(element.project).findFile(file) ?: return emptyList()
        return psiFile.getUserData(YAML_DART_PACKAGE_INFO_KEY) ?: emptyList()
    }

    //获取 pub数据
    fun tryGetPackageInfo(): PubVersionDataModel? {
        val pluginName = getDartPluginNameAndVersion()?.name ?: return null
        val models = tryGetModels()
        return models.find { it.name == pluginName }?.pubData
    }
}

object MyYamlPsiElementFactory {

    ///创建一个节点
    fun createPlainPsiElement(project: Project, text: String): YAMLPlainTextImpl? {
        val instance = PsiFileFactory.getInstance(project)
        val createFileFromText: YAMLFile = instance.createFileFromText(YAMLLanguage.INSTANCE, "name: $text") as YAMLFile
        return PsiTreeUtil.findChildOfType(createFileFromText, YAMLPlainTextImpl::class.java)
    }
}