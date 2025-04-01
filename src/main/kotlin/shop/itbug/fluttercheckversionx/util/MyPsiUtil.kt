package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.project.stateStore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.model.FlutterPluginType
import java.io.File


/**
 * PSI 操作相关类
 */
class MyPsiElementUtil {

    companion object {

        /**
         * 插入节点到pubspec文件
         */
        fun insertPluginToPubspecFile(
            project: Project,
            pluginName: String,
            version: String = "any",
            type: FlutterPluginType = FlutterPluginType.Dependencies
        ) {
            val psiFile = getPubSpecYamlFile(project)
            if (psiFile != null) {
                val qualifiedKeyInFile =
                    runReadAction { YAMLUtil.getQualifiedKeyInFile(psiFile as YAMLFile, type.type) }
                val insetVersion = "^$version"
                val blockElement = runReadAction {
                    YAMLElementGenerator.getInstance(project)
                        .createYamlKeyValue(pluginName, insetVersion)
                }
                val eolElement = runReadAction { YAMLElementGenerator.getInstance(project).createEol() }
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
        fun getPubSpecYamlFile(project: Project): PsiFile? {
            val pubspecYamlFile =
                LocalFileSystem.getInstance()
                    .findFileByIoFile(File("${project.stateStore.projectBasePath}/pubspec.yaml"))
            if (pubspecYamlFile != null) {
                return runReadAction { PsiManager.getInstance(project).findFile(pubspecYamlFile) }
            }
            return null
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
fun PsiElement.isDartPluginElement(): Boolean {
    if (this is YAMLKeyValueImpl && this.parent.parent is YAMLKeyValueImpl) {
        val p = this.parent.parent as YAMLKeyValueImpl
        val temp = p.keyText
        return temp == "dependencies" || temp == "dependency_overrides" || temp == "dev_dependencies"
    }
    return false
}


fun PsiElement.getPluginName(): String {
    return MyPsiElementUtil.getPluginNameWithPsi(this)
}

/**
 * 获取项目下的pubspec.yaml文件的yaml file对象
 */
fun Project.getPubspecYAMLFile(): YAMLFile? {
    return MyPsiElementUtil.getPubSpecYamlFile(this) as? YAMLFile
}