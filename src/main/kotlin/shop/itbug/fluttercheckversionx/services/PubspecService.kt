package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    
    // StateFlow for Compose
    private val _dependenciesNamesFlow = MutableStateFlow<List<String>>(emptyList())
    val dependenciesNamesFlow: StateFlow<List<String>> = _dependenciesNamesFlow.asStateFlow()
    
    private val _detailsFlow = MutableStateFlow<List<DartYamlModel>>(emptyList())
    val detailsFlow: StateFlow<List<DartYamlModel>> = _detailsFlow.asStateFlow()


    fun getAllPackages() = details

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
            
            // Update flows
            _detailsFlow.value = all
            _dependenciesNamesFlow.value = dependenciesNames
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
        return hasDependencies("freezed") || hasDependencies("freezed_annotation")
    }

    //判断 freezed 或者freezed_annotation 使用的版本号是不是大于 3 或者等于 3
    fun freezedVersionIsThan3(): Boolean {
        if (!hasFreezed()) return false
        val freezedVersion = details.find { it.name == "freezed" }
        val freezedAnnotationVersion = details.find { it.name == "freezed_annotation" }

        val versionToCheck = when {
            freezedVersion != null -> freezedVersion
            freezedAnnotationVersion != null -> freezedAnnotationVersion
            else -> return false
        }

        if (versionToCheck.versionType == DartVersionType.Any) return true
        return isVersionGreaterThanThree(versionToCheck.version, Version.parse("3.0.0"))
    }


    /**
     * 检测依赖是否使用了[pluginName]这个包
     */
     fun hasDependencies(pluginName: String): Boolean {
        return dependenciesNames.contains(pluginName)
    }




    companion object {
        fun getInstance(project: Project): PubspecService {
            return project.service<PubspecService>()
        }
    }

    override fun dispose() {
        job.cancel()
        dependenciesNames = emptyList()
        details = emptyList()
        _dependenciesNamesFlow.value = emptyList()
        _detailsFlow.value = emptyList()
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default


    var reCheckJob: Job? = null

    fun reCheck() {
        if (reCheckJob?.isActive == true) {
            reCheckJob?.cancel()
        }
        reCheckJob = launch(Dispatchers.IO) {
            startCheck()
        }
    }

}


class PubspecServiceFileListener(val project: Project) : BulkFileListener {
    override fun after(events: List<out VFileEvent>) {
        events.find { it.file?.name == "pubspec.yaml" } ?: return
        val service = PubspecService.getInstance(project)
        service.reCheck()
        super.after(events)
    }
}


