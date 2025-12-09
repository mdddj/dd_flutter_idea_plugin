package shop.itbug.fluttercheckversionx.common.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.services.MyPackageGroup

// 判断项目是否为 flutter 项目
suspend fun Project.isFlutterProject(): Boolean {
    val baseDir = guessProjectDir() ?: return false
    val pubFile = baseDir.findChild("pubspec.yaml") ?: return false
    val pubPsiFile = PsiManager.getInstance(this).findFile(pubFile) as? YAMLFile? ?: return false
    return PubspecYamlFileTools.create(pubPsiFile).isFlutterProject()
}

fun Project.hasPubspecYamlFile(): Boolean {
    val baseDir = guessProjectDir() ?: return false
    return baseDir.findChild("pubspec.yaml") != null
}

/**
 * flutter yaml 操作相关工具函数
 */
class PubspecYamlFileTools private constructor(yaml: YAMLFile) : YamlFileToolBase(yaml) {

    /**
     * 判断是否为flutter项目,这四个是flutter项目必填字段
     * && hasKey("description") && hasKey("version")
     */
    suspend fun isFlutterProject() =
        hasKey("name") && hasKey("environment")


    /**
     * 获取插件列表
     */
    suspend fun getDependencies() = getChsWith("dependencies")
    suspend fun getDevDependencies() = getChsWith("dev_dependencies")
    suspend fun getDependencyOverrides() = getChsWith("dependency_overrides")


    /**
     * 获取全部的插件模型
     */
    suspend fun allDependencies() =
        (getDependencies().map {
            DartYamlModel.create(it)?.copy(
                type = MyPackageGroup.Dependencies
            )
        } + getDevDependencies().map {
            DartYamlModel.create(it)?.copy(type = MyPackageGroup.DevDependencies)
        } + getDependencyOverrides().map {
            DartYamlModel.create(it)?.copy(type = MyPackageGroup.DependencyOverrides)
        }).filterNotNull()

    /**
     * 获取插件列表模型
     */
    suspend fun getDependenciesModel(list: List<YAMLKeyValueImpl>): List<DartYamlModel> {
        return coroutineScope {
            list.map { async { DartYamlModel.fetch(it) } }.awaitAll().filterNotNull()
        }
    }

    /**
     * 获取全部dart pub 模型
     */
    suspend fun getAllDependenciesList(): List<DartYamlModel> = coroutineScope {
        val deps = async { getDependenciesModel(getDependencies()) }
        val devDeps = async { getDependenciesModel(getDevDependencies()) }
        val overrideDeps = async { getDependenciesModel(getDependencyOverrides()) }
        deps.await() + devDeps.await() + overrideDeps.await()
    }

    companion object {
        fun create(file: YAMLFile) = PubspecYamlFileTools(file)
    }

}

