package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.project.stateStore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.IncorrectOperationException
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.constance.igFlutterPlugin
import shop.itbug.fluttercheckversionx.model.FlutterPluginElementModel
import shop.itbug.fluttercheckversionx.model.FlutterPluginType
import shop.itbug.fluttercheckversionx.tools.FlutterVersionTool
import java.io.File


/**
 * PSI 操作相关类
 */
class MyPsiElementUtil {

    companion object {

        /**
         * 插入节点到pubspec文件
         */
        suspend fun insertPluginToPubspecFile(
            project: Project,
            pluginName: String,
            version: String = "any",
            type: FlutterPluginType = FlutterPluginType.Dependencies
        ) {
            val psiFile = getPubSpecYamlFile(project)
            if (psiFile != null) {
                val qualifiedKeyInFile = YAMLUtil.getQualifiedKeyInFile(psiFile as YAMLFile, type.type)
                val insetVersion = "^$version"
                val blockElement = YAMLElementGenerator.getInstance(project)
                    .createYamlKeyValue(pluginName, insetVersion)
                val eolElement = YAMLElementGenerator.getInstance(project).createEol()
                WriteCommandAction.runWriteCommandAction(project) {
                    try {
                        qualifiedKeyInFile?.add(eolElement)
                        qualifiedKeyInFile?.add(blockElement)
                    } catch (e: IncorrectOperationException) {
                        project.toastWithError("add to file error: $e")
                    }
                }
            }
        }


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

        /**
         * 获取项目pubspec.yaml 文件
         */
        suspend fun getPubSpecYamlFile(project: Project): PsiFile? {
            FlutterVersionTool.readVersionFromSdkHome(project) ?: return null
            val pubspecYamlFile =
                LocalFileSystem.getInstance()
                    .findFileByIoFile(File("${project.stateStore.projectBasePath}/pubspec.yaml"))
            if (pubspecYamlFile != null) {
                return readAction { PsiManager.getInstance(project).findFile(pubspecYamlFile) }
            }
            return null
        }

        /**
         * 获取项目的所有插件
         */
        suspend fun getAllPlugins(project: Project, key: String = "dependencies"): List<String> {
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
        suspend fun getAllFlutters(project: Project): MutableMap<FlutterPluginType, List<FlutterPluginElementModel>> {
            val yamlFile = project.getPubspecYAMLFile()
            val map = mutableMapOf<FlutterPluginType, List<FlutterPluginElementModel>>()
            yamlFile?.let { yaml ->
                val coreElement = yaml.firstChild.firstChild
                val coreElementChildren = coreElement.childrenOfType<YAMLKeyValueImpl>()
                if (coreElementChildren.isNotEmpty()) {
                    FlutterPluginType.entries.forEach { type ->
                        val l = coreElementChildren.filter { it.keyText == type.type }.toList()
                        if (l.isNotEmpty()) {
                            val pluginDevs = l.first()
                            if (pluginDevs.childrenOfType<YAMLBlockMappingImpl>().isNotEmpty()) {
                                val mapping = pluginDevs.childrenOfType<YAMLBlockMappingImpl>().first()
                                var psis = mapping.childrenOfType<YAMLKeyValueImpl>()
                                val igs = igFlutterPlugin.igPlugins
                                psis = psis.filter { p -> !igs.contains(p.keyText) }
                                map[type] = psis.map { psi ->
                                    FlutterPluginElementModel(
                                        name = psi.keyText,
                                        type = type,
                                        element = psi
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return map
        }

        fun modifyPsiElementText(psiElement: PsiElement, newText: String) {
            // 使用WriteCommandAction来执行修改操作
            WriteCommandAction.runWriteCommandAction(psiElement.project) {

                // 获取当前Psi文件的文档对象
                val document = PsiDocumentManager.getInstance(psiElement.project)
                    .getDocument(psiElement.containingFile)
                if (document != null) {
                    // 获取要修改的文本范围
                    val startOffset = psiElement.textRange.startOffset
                    val endOffset = psiElement.textRange.endOffset
                    PsiDocumentManager.getInstance(psiElement.project).doPostponedOperationsAndUnblockDocument(document)
                    // 替换文本内容
                    document.replaceString(startOffset, endOffset, newText)
                    // 提交更改并更新Psi文件
                    PsiDocumentManager.getInstance(psiElement.project).doPostponedOperationsAndUnblockDocument(document)
//                    PsiDocumentManager.getInstance(psiElement.project).commitDocument(document)
                }
            }
        }

        fun findAllMatchingElements(
            psiElement: PsiElement,
            matchText: (text: String, psiElement: PsiElement) -> Boolean
        ): List<PsiElement> {
            val matchingElements: MutableList<PsiElement> = ArrayList()
            if (matchText(psiElement.text, psiElement)) {
                matchingElements.add(psiElement)
            }
            val children = psiElement.children
            for (child in children) {
                val childMatchingElements: List<PsiElement> = findAllMatchingElements(child, matchText)
                matchingElements.addAll(childMatchingElements)
            }

            return matchingElements
        }

    }


}


fun PsiElement.exByModifyPsiElementText(newText: String) {
    MyPsiElementUtil.modifyPsiElementText(this, newText)
}


/**
 * 判断此节点是否为flutter插件
 */
suspend fun PsiElement.isDartPluginElement(): Boolean {

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


fun PsiElement.getPluginName(): String {
    return MyPsiElementUtil.getPluginNameWithPsi(this)
}

/**
 * 获取项目下的pubspec.yaml文件的yaml file对象
 */
suspend fun Project.getPubspecYAMLFile(): YAMLFile? {
    return MyPsiElementUtil.getPubSpecYamlFile(this) as? YAMLFile
}