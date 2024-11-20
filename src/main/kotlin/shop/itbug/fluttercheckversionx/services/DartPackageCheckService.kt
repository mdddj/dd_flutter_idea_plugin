package shop.itbug.fluttercheckversionx.services

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.table.JBTable
import com.intellij.util.messages.Topic
import kotlinx.coroutines.*
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName
import shop.itbug.fluttercheckversionx.util.DateUtils
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.YamlExtends
import javax.swing.table.DefaultTableModel


class DartPackageCheckActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        DartPackageCheckService.getInstance(project).start()
    }
}

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

    /**
     * 是否可以升级
     */
    fun hasNew(): Boolean {
        val packageName = first.getDartPluginVersionName()
        val second = second ?: return false
        return second.getLastVersionText(packageName) != null
    }

    /**
     * 获取最新版本
     */
    fun serverLastVersion(): String? {
        return second?.getLastVersionText(first.getDartPluginVersionName())
    }

    /**
     * 获取最后更新时间
     */
    fun getLastUpdateTime(): String {
        val time = second?.lastVersionUpdateTimeString ?: return ""
        return DateUtils.timeAgo(time)
    }

    /**
     * 获取最后更新时间
     */
    fun getLastUpdateTimeFormatString(): String {
        val time = second?.lastVersionUpdateTimeString ?: return ""
        return DateUtils.timeAgo(time)
    }

    /**
     * 读取表格行数据
     */
    fun getTableRowData(): Array<Any> {
        return arrayOf(first.packageName, first.detail.version, this, getLastUpdateTime())
    }
}

data class MyDartPackage(
    var packageName: String,
    val element: YAMLKeyValueImpl,
    val detail: DartPluginVersionName,
    val versionElement: YAMLPlainTextImpl,
    val error: String? = null,
    val service: DartPackageCheckService? = null,
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

    fun getDartPluginVersionName(): DartPluginVersionName {
        return detail
    }

    ///替换成功了
    fun replaced(newVersion: String, newTextElement: YAMLPlainTextImpl) {
        if (service != null) {
            var find = service.details.firstOrNull { it.first == this }
            val newObject = this.copy(detail = this.detail.copy(version = newVersion), versionElement = newTextElement)
            if (find != null) {
                find = find.copy(first = newObject)
                val index = service.details.indexOfFirst { it.first.packageName == this.packageName }
                if (index >= 0) {
                    service.details[index] = find
                }

            }
        }
    }
}

typealias DartCheckTaskComplete = () -> Unit

data class DartPackageTaskParam(
    val showNotification: Boolean = true,
    val complete: DartCheckTaskComplete? = null
)

/**
 * 项目包检测的服务类
 */
@Service(Service.Level.PROJECT)
class DartPackageCheckService(val project: Project) {
    private val ignoreManager = DartPluginIgnoreConfig.getInstance(project)
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
                        service = this@DartPackageCheckService, group = group
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
        val list = getPackageInfos().filter { ignoreManager.isIg(it.packageName).not() }
        val results = runBlocking {
            list.map { async(Dispatchers.IO) { it.getDetailApi() } }.awaitAll()
        }
        this.details = results.toMutableList()
        project.messageBus.syncPublisher(FetchDartPackageFinishTopic).finish(results)//发送加载完成通知
        MyFileUtil.reIndexPubspecFile(project)//重新索引
    }


    @OptIn(DelicateCoroutinesApi::class)
    suspend fun startWithAsync(param: DartPackageTaskParam = DartPackageTaskParam()) {
        val startTime = System.currentTimeMillis()  // 获取起始时间
        this.details.clear()
        val list = getPackageInfos().filter { ignoreManager.isIg(it.packageName).not() }
        val results = GlobalScope.async {
            list.map { async(Dispatchers.IO) { it.getDetailApi() } }.awaitAll()
        }.await()
        this.details = results.toMutableList()
        val endTime = System.currentTimeMillis()  // 获取结束时间
        GlobalScope.launch(Dispatchers.Main) {
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
     * 判断是否有新版本
     */
    fun hasNew(item: PubPackage): Boolean {
        return item.hasNew()
    }


    /**
     * 删除某一个项目
     */
    fun removeItemByPluginName(name: String) {
        val find = details.find { it.first.packageName == name }
        if (find != null) {
            details.remove(find)
        }
    }

    /**
     * 重新索引
     */
    suspend fun resetIndex(param: DartPackageTaskParam = DartPackageTaskParam()) {
        startWithAsync(param)
    }


    /**
     * 从api请求插件信息
     */
    fun fetchPluginDateFromApi(element: PsiElement, packageName: String): PubPackage? {
        val packInfo = runReadAction { YamlExtends(element).getMyDartPackageModel() } ?: return null
        val r = PubService.callPluginDetails(packageName)
        return PubPackage(packInfo.copy(service = this), r)
    }

    /**
     * 插件名查找已经获取到包信息
     */
    fun findPackageInfoByName(pluginName: String): PubPackage? {
        return details.find { it.first.packageName == pluginName }
    }

    fun getTableRows(): List<Array<String>> {
        return details.map {
            val date = it.second?.lastVersionUpdateTimeString ?: ""
            val timeAgo = if (date.isBlank()) "-" else DateUtils.timeAgo(date)
            arrayOf(
                it.first.packageName, it.first.detail.version, it.second?.latest?.version ?: "-", "$date ($timeAgo)"
            )
        }
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
        fun getInstance(project: Project): DartPackageCheckService {
            return project.service<DartPackageCheckService>()
        }

        /**
         * 获取表格的列
         */
        fun getTableColumns(): Array<String> {
            return arrayOf("Name", "Current Version(in file)", "Last Version(Pub.dev)", "Last Update")
        }

        /**
         * 设置表格的列宽度
         */
        fun setColumnWidth(table: JBTable) {
            table.columnModel.let {
                it.getColumn(0).minWidth = 250
                it.getColumn(1).minWidth = 200
                it.getColumn(2).minWidth = 200
                it.getColumn(3).minWidth = 300
            }
        }


        /**
         * 设置表格数据
         */
        fun setJBTableData(table: JBTable, project: Project) {
            val model = DefaultTableModel(getTableColumns(), 0)
            val service = getInstance(project)
            service.details.map {
                model.addRow(it.getTableRowData())
            }
            table.model = model
        }

        fun getNotificationGroup(): NotificationGroup? {
            val id = "dart_package_check_service"
            return NotificationGroupManager.getInstance().getNotificationGroup(id)
        }
    }
}

