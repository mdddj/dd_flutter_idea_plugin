package shop.itbug.fluttercheckversionx.services

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.messages.Topic
import kotlinx.coroutines.*
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.YamlExtends


/**
 * 包类型
 */
enum class MyPackageGroup(val baseName: String) {
    Dependencies("dependencies"), DevDependencies("dev_dependencies"), DependencyOverrides("dependency_overrides");

    override fun toString(): String {
        return baseName
    }
}


data class PubPackage(
    val first: MyDartPackage,
    val second: PubVersionDataModel?,
) {

    override fun toString(): String {
        return first.packageName
    }

}

data class MyDartPackage(
    var packageName: String,
    val element: YAMLKeyValueImpl,
    val detail: DartPluginVersionName,
    val versionElement: YAMLPlainTextImpl,
    val error: String? = null,
    val group: MyPackageGroup = MyPackageGroup.Dependencies
) {
    /**
     * 通过 api来向 pub加载插件的数据
     */
    fun getDetailApi(): PubPackage {
        try {
            val r = PubService.callPluginDetails(packageName)
            return PubPackage(this, r)
        } catch (e: Exception) {
            return PubPackage(this.copy(error = e.localizedMessage), null)
        }
    }


}

typealias DartCheckTaskComplete = () -> Unit

data class DartPackageTaskParam(
    val showNotification: Boolean = true, val complete: DartCheckTaskComplete? = null
)

/**
 * 项目包检测的服务类
 */
@Service(Service.Level.PROJECT)
@Deprecated("不建议使用了这个")
class DartPackageCheckService(val project: Project) : Disposable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pubspecFile: YAMLFile? = null
    var details: MutableList<PubPackage> = mutableListOf()///从服务器获取的数据
    var projectName: String = "Flutter App"


    /**
     * 读取项目的包文件,pubspec.yaml
     */
    private fun getPubspecFile(): YAMLFile? {
        if (pubspecFile != null) {
            return pubspecFile
        }
        val projectDir = project.guessProjectDir() ?: return null
        val pubspecFile = projectDir.findChild("pubspec.yaml") ?: return null
        val file = ApplicationManager.getApplication().runReadAction(Computable<PsiFile?> {
            PsiManager.getInstance(project).findFile(pubspecFile)
        }) as YAMLFile
        this.pubspecFile = file
        val nameElement = runReadAction { YAMLUtil.getQualifiedKeyInFile(file, "name") }
        if (nameElement != null) {
            this.projectName = nameElement.valueText
        }
        return file
    }

    /**
     * 解析插件列表,不要在这里执行耗时的操作
     */
    fun getPackageInfos(): List<MyDartPackage> {
        val pubspecFile = getPubspecFile() ?: return emptyList()
        val r = runBlocking {
            MyPackageGroup.entries.map {
                async {
                    collectDependencies(it, pubspecFile)
                }
            }.awaitAll()
        }
        return r.flatten()
    }


    /**
     * 收集包并分组
     */
    private fun collectDependencies(group: MyPackageGroup, file: YAMLFile): List<MyDartPackage> {
        val deps = runReadAction { YAMLUtil.getQualifiedKeyInFile(file, group.baseName) } ?: return emptyList()
        val block = deps.value as? YAMLBlockMappingImpl ?: return emptyList()
        val packageElements = runReadAction { PsiTreeUtil.findChildrenOfType(block, YAMLKeyValueImpl::class.java) }
        val r = runBlocking {
            packageElements.map {
                async(Dispatchers.EDT) {
                    YamlExtends(it).getMyDartPackageModel()?.copy(
                    )
                }
            }.awaitAll()
        }
        return r.filterNotNull()
    }

    /**
     * 开始检测包,然后远程检测包的新版本信息
     */
    fun start() {
        val list = getPackageInfos()
        val results = runBlocking {
            list.map { async(Dispatchers.IO) { it.getDetailApi() } }.awaitAll()
        }
        this.details = results.toMutableList()
        project.messageBus.syncPublisher(FetchDartPackageFinishTopic).finish(results)//发送加载完成通知
        MyFileUtil.reIndexPubspecFile(project)//重新索引
    }


    suspend fun startWithAsync(param: DartPackageTaskParam = DartPackageTaskParam()) {
        val startTime = System.currentTimeMillis()  // 获取起始时间
        this.details.clear()
        val list = getPackageInfos()
        val results = withContext(Dispatchers.IO) {
            list.map { async(Dispatchers.IO) { it.getDetailApi() } }.awaitAll()
        }
        this.details = results.toMutableList()
        val endTime = System.currentTimeMillis()  // 获取结束时间
        scope.launch(Dispatchers.Main) {
            project.messageBus.syncPublisher(FetchDartPackageFinishTopic).finish(results)///发送加载完成通知
            if (param.showNotification) {
                getNotificationGroup()?.createNotification(
                    PluginBundle.get("refresh_success") + ", ${PluginBundle.get("package_size_is")}:${details.size} (${endTime - startTime}ms)",
                    NotificationType.INFORMATION
                )?.notify(project)
            }
            param.complete?.invoke()
        }
        MyFileUtil.reIndexPubspecFile(project)//重新索引
    }


    /**
     * 重新索引
     */
    private suspend fun resetIndex(param: DartPackageTaskParam = DartPackageTaskParam()) {
        startWithAsync(param)
    }


    fun startResetIndex(params: DartPackageTaskParam = DartPackageTaskParam()) {
        scope.launch {
            resetIndex(params)
        }
    }


    override fun dispose() {
        scope.cancel()
    }

    /**
     * 包数据加载完成事件
     */
    interface FetchDartPackageFinish {
        fun finish(details: List<PubPackage>)
    }

    companion object {


        val FetchDartPackageFinishTopic: Topic<FetchDartPackageFinish> =
            Topic.create("FetchDartPackageFinishTopic", FetchDartPackageFinish::class.java)

        /**
         * 读取项目包操作模块的实例
         */
        @Deprecated("已弃用")
        fun getInstance(project: Project): DartPackageCheckService {
            return project.service<DartPackageCheckService>()
        }


        fun getNotificationGroup(): NotificationGroup? {
            val id = "dart_package_check_service"
            return NotificationGroupManager.getInstance().getNotificationGroup(id)
        }
    }
}

