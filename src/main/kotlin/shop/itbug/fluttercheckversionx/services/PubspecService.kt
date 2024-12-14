package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import shop.itbug.fluttercheckversionx.common.yaml.PubspecYamlFileTools
import shop.itbug.fluttercheckversionx.util.MyFileUtil


class PubspecStartActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        PubspecService.getInstance(project).startCheck()
    }
}


/**
 * 仅检测主pubspec.yaml
 */
@Service(Service.Level.PROJECT)
class PubspecService(val project: Project) : Disposable {

    private var dependenciesNames = listOf<String>()


    fun getAllDependencies(): List<String> = dependenciesNames

    /**
     * 读取项目使用了哪些包?
     */
    suspend fun startCheck() {
        val file = readAction { MyFileUtil.getPubspecFile(project) }
        if (file != null) {
            val tool = PubspecYamlFileTools.create(file)
            val all = tool.allDependencies()
            dependenciesNames = all.map { it.name }
        }
    }


    /**
     * 项目是否引入 riverpod 包
     */
    fun hasRiverpod(): Boolean {
        return hasDependencies("hooks_riverpod") || hasDependencies("riverpod_annotation") || hasDependencies("riverpod_annotation") || hasDependencies(
            "riverpod_generator"
        )
    }


    /**
     * 项目是否使用 provider 包
     */
    fun hasProvider(): Boolean {
        return hasDependencies("provider")
    }


    /**
     * 检测依赖是否使用了[pluginName]这个包
     */
    private fun hasDependencies(pluginName: String): Boolean {
        return dependenciesNames.contains(pluginName)
    }

    companion object {
        fun getInstance(project: Project): PubspecService {
            return project.service<PubspecService>()
        }
    }

    override fun dispose() {
        println("-----dispose--------PubspecService")
        dependenciesNames = emptyList()
    }

}

