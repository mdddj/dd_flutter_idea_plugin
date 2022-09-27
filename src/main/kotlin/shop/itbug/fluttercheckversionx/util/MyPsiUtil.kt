package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.project.stateStore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import shop.itbug.fluttercheckversionx.model.FlutterPluginElementModel
import shop.itbug.fluttercheckversionx.model.FlutterPluginType
import java.io.File

typealias FindYamlKeyValueExpression = (keyValueElement: YAMLKeyValueImpl) -> Boolean

/**
 * PSI 操作相关类
 */
class MyPsiElementUtil {

    companion object {

        /**
         * 获取插件名字
         *
         * 例子:
         * flutter_launcher_icons: ^0.9.2
         * 返回 flutter_launcher_icons
         */
        fun getPluginNameWithPsi(psiElement: PsiElement?): String {
            if (psiElement is YAMLKeyValueImpl) {
                return psiElement.keyText
            }
            if (psiElement is LeafPsiElement) {
                psiElement.text
            }
            return psiElement?.text ?: ""
        }

        fun hasYamlMapping(element: PsiElement): Boolean {
            return element.children.filterIsInstance<YAMLMappingImpl>().isNotEmpty()
        }

        /**
         * 获取项目pubspec.yaml 文件
         */
        fun getPubSecpYamlFile(project: Project): PsiFile? {
            val pubspecYamlFile =
                LocalFileSystem.getInstance()
                    .findFileByIoFile(File("${project.stateStore.projectBasePath}/pubspec.yaml"))
            if (pubspecYamlFile != null) {
                return PsiManager.getInstance(project).findFile(pubspecYamlFile)
            }
            return null
        }

        /**
         * 获取项目的所有插件
         */
        fun getAllPlugins(project: Project, key: String = "dependencies"): List<String> {
            val yamlFile = project.getPubspecYAMLFile()
            yamlFile?.let { file ->
                val deps = YAMLUtil.getQualifiedKeyInFile(file, key)
                if (deps != null) {
                    return deps.children.first().children.map { (it as YAMLKeyValueImpl).keyText }
                }
            }
            return emptyList()
        }

        /**
         * 获取项目插件列表
         */
        fun getAllFlutters(project: Project): MutableMap<FlutterPluginType, List<FlutterPluginElementModel>> {
            val yamlFile = project.getPubspecYAMLFile()
            val map = mutableMapOf<FlutterPluginType, List<FlutterPluginElementModel>>()
            yamlFile?.let { yaml ->
                val coreElement = yaml.firstChild.firstChild
                val coreElementChildrens = coreElement.childrenOfType<YAMLKeyValueImpl>()
                FlutterPluginType.values().forEach { type ->
                    val pluginDevs = coreElementChildrens.filter { it.keyText == type.type }.toList().first()
                    val mapping = pluginDevs.childrenOfType<YAMLBlockMappingImpl>().first()
                    map[type] = mapping.childrenOfType<YAMLKeyValueImpl>().map { psi ->
                        FlutterPluginElementModel(
                            name = psi.keyText,
                            type = type,
                            element = psi
                        )
                    }
                }
            }
            return map
        }


    }


}


/**
 * 判断此节点是否为flutter插件
 */
fun PsiElement.isDartPluginElement(): Boolean {

    if (this is YAMLKeyValueImpl) {
        val allPluginsMap = MyPsiElementUtil.getAllFlutters(project)
        val allPlugins = allPluginsMap.values.toList()
        var isin = false
        allPlugins.forEach { e1 ->
            try {
                e1.first { it.name == this.keyText }
                isin = true
            } catch (_: Exception) {
            }
        }
        return isin
    }
    return false
}


fun YAMLKeyValueImpl.isDartPluginElementWithKeyValue(): Boolean {
    if (this.parents(false).iterator().hasNext() && this.parents(false)
            .iterator().next().parent is YAMLKeyValueImpl
    ) {
        val ds = listOf("dependencies", "dev_dependencies", "dependency_overrides")
        val parentKeyText = (this.parents(false).iterator().next().parent as YAMLKeyValueImpl).keyText
        if (ds.contains(parentKeyText) && this.keyText != "flutter" && this.keyText != "flutter_test") {
            return true
        }
    }
    return false
}

fun PsiElement.getPluginName(): String {
    return MyPsiElementUtil.getPluginNameWithPsi(this)
}

/**
 * 获取项目下的pubspec.yaml文件的yamlfile对象
 */
fun Project.getPubspecYAMLFile(): YAMLFile? {
    return MyPsiElementUtil.getPubSecpYamlFile(this) as? YAMLFile
}