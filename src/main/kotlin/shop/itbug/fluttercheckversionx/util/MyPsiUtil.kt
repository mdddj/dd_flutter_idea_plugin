package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.project.stateStore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.elementType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import java.io.File

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
            if (psiElement != null) {
                val text = psiElement.text
                if (text != null) {
                    if ( psiElement is YAMLKeyValueImpl && (text.contains(": ^") || text.contains(": any")) ) {
                        return text.split(":")[0]
                    }
                }
            }
            return ""
        }

        /**
         * 获取项目pubspec.yaml 文件
         */
        fun getPubSecpYamlFile(project: Project) : PsiFile? {
            val pubspecYamlFile =
                LocalFileSystem.getInstance().findFileByIoFile(File("${project.stateStore.projectBasePath}/pubspec.yaml"))
            if(pubspecYamlFile!=null) {
                return PsiManager.getInstance(project).findFile(pubspecYamlFile)
            }
            return null
        }

        /**
         * 获取项目的所有插件
         */
        fun getAllPlugins(project: Project): List<String> {
            val pubSecpYamlFile = getPubSecpYamlFile(project)
            if(pubSecpYamlFile!=null){
                val deps = YAMLUtil.getQualifiedKeyInFile(pubSecpYamlFile as YAMLFile, "dependencies")
                if (deps != null) {
                    println(deps.children.first().children.size)
                    val plugins = deps.children.first().children.map { (it as YAMLKeyValueImpl).keyText }
                    println("获取项目的所有插件:$plugins")
                    return plugins
                }
            }
            return emptyList()
        }
    }
}