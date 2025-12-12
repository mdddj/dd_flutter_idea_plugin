package shop.itbug.flutterx.services.noused

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import java.nio.file.Path


/**
 * 获取包模型
 */
fun VirtualFile.getDartNoUsedModel(project: Project): DartNoUsedModel? {
    val pubspecName = "pubspec.yaml"
    val pubspecYamlFile: VirtualFile =
        (if (name == "lib") parent.findChild(pubspecName) else findChild(pubspecName)) ?: return null
    val vf = VirtualFileManager.getInstance().findFileByNioPath(Path.of(pubspecYamlFile.path)) ?: return null
    val psi = PsiManager.getInstance(project).findFile(vf) as? YAMLFile ?: return null
    val nameEle = YAMLUtil.getQualifiedKeyInFile(psi, "name") ?: return null
    val packName = nameEle.valueText
    val packPath = if (name == "lib") parent.path else path
    val versionEle = YAMLUtil.getQualifiedKeyInFile(psi, "version")
    val version = versionEle?.valueText ?: ""
    val model = DartNoUsedModel(
        packageName = packName,
        packageDirectory = packPath,
        version = version
    )
    return model
}

/**
 * 模型
 */
data class DartNoUsedModel(
    /**
     * 包的名称
     */
    var packageName: String,


    /**
     * 版本号
     */
    var version: String,

    /**
     * 包的项目路径
     */
    var packageDirectory: String
) {
    override fun toString(): String {
        return "name=$packageName, version = $version"
    }
}