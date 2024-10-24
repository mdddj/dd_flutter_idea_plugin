package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil


class PubspecStartActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        PubspecService.getInstance(project).startCheck()
        VirtualFileManager.getInstance()
            .addAsyncFileListener(PubspecFileChangeListenAsync(project), project.service<PubspecService>())
    }
}

@Service(Service.Level.PROJECT)
class PubspecService(val project: Project) : Disposable {

    private var dependenciesNames = listOf<String>()


    fun getAllDependencies(): List<String> = dependenciesNames

    /**
     * 读取项目使用了哪些包?
     */
    fun startCheck() {
        runReadAction {
            dependenciesNames = MyPsiElementUtil.getAllPluginsString(project)
        }
    }


    /**
     * 项目是否引入 riverpod 包
     */
    fun hasRiverpod(): Boolean {
        return hasDependencies("hooks_riverpod")
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


/**
 * 监听pubspec.yaml文件被修改,重新索引它
 */
class PubspecFileChangeListenAsync(val project: Project) : AsyncFileListener {
    @OptIn(DelicateCoroutinesApi::class)
    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        if (project.isDisposed) {
            return null
        }
        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                if (project.isDisposed) {
                    return
                }
                events.forEach {
                    val filename = it.file?.name
                    if (filename != null && filename == "pubspec.yaml") {
                        println("文件发生变化::::::::::${filename}")
                        it.file?.let { file -> MyFileUtil.reIndexFile(file) }
                    }
                }

            }
        }
    }

}