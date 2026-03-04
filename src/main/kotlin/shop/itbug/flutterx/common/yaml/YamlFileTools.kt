package shop.itbug.flutterx.common.yaml

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

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


    private val YAMLKeyValueImpl.point
        get() = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)

    /**
     * 获取全部的插件模型
     */
    suspend fun allDependencies(): List<DartYamlModel> = coroutineScope {
        val deps = getDependencies()
        val devDeps = getDevDependencies()
        val overrideDeps = getDependencyOverrides()

        val pointers = readAction {
            (deps + devDeps + overrideDeps)
                .filter { it.isValid }
                .map { SmartPointerManager.createPointer(it) }
        }

        pointers.map { ptr ->
            async { DartYamlModel.create(ptr) }
        }.awaitAll().filterNotNull()
    }

    suspend fun getDependenciesModel(list: List<YAMLKeyValueImpl>): List<DartYamlModel> {
        val pointers = readAction {
            list.filter { it.isValid }.map { SmartPointerManager.createPointer(it) }
        }
        return coroutineScope {
            pointers.map { ptr ->
                async { DartYamlModel.fetch(ptr) }
            }.awaitAll().filterNotNull()
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

