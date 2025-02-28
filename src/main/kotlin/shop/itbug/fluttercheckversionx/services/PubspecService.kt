package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.common.yaml.DartYamlModel
import shop.itbug.fluttercheckversionx.common.yaml.PubspecYamlFileTools
import shop.itbug.fluttercheckversionx.util.DartVersionType
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.Version
import shop.itbug.fluttercheckversionx.util.isVersionGreaterThanThree
import kotlin.coroutines.CoroutineContext


class PubspecStartActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        PubspecService.getInstance(project).startCheck()
    }
}


/**
 * 仅检测主pubspec.yaml
 */
@Service(Service.Level.PROJECT)
class PubspecService(val project: Project) : Disposable, CoroutineScope {

    private val job = Job()
    private var dependenciesNames = listOf<String>()
    private var details = listOf<DartYamlModel>()
    private val listen = MyPubspecServiceListenFileChange(this, project)

    fun getAllDependencies(): List<String> = dependenciesNames

    init {
        VirtualFileManager.getInstance().addVirtualFileListener(listen)
    }

    /**
     * 读取项目使用了哪些包?
     */
    suspend fun startCheck() {
        val file = readAction { MyFileUtil.getPubspecFile(project) }
        if (file != null) {
            val tool = PubspecYamlFileTools.create(file)
            val all = tool.allDependencies()
            details = all
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


    //是否使用了 freezed包
    fun hasFreezed(): Boolean {
        return hasDependencies("freezed")
    }

    //判断 freezed使用的版本号是不是大于 3 或者等于 3
    fun freezedVersionIsThan3(): Boolean {
        if (!hasFreezed()) return false
        val version = details.find { it.name == "freezed" } ?: return false
        if (version.versionType == DartVersionType.Any) return true
        return isVersionGreaterThanThree(version.version, Version.parse("3.0.0"))
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
        job.cancel()
        VirtualFileManager.getInstance().removeVirtualFileListener(listen)
        dependenciesNames = emptyList()
        details = emptyList()
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

}


///监听 pubspec.yaml文件的变化，【PubspecService】重新检查
class MyPubspecServiceListenFileChange(val coroutineScope: CoroutineScope, val project: Project) : VirtualFileListener {
    override fun contentsChanged(event: VirtualFileEvent) {
        super.contentsChanged(event)
        if (event.fileName == "pubspec.yaml") {
            coroutineScope.launch {
                handleFileChanged(event)
            }
        }
    }

    private suspend fun handleFileChanged(event: VirtualFileEvent) {
        withContext(Dispatchers.IO) {
            println("文件发生变化，重新检测包管理服务")
            PubspecService.getInstance(project).startCheck()
        }
    }

}
