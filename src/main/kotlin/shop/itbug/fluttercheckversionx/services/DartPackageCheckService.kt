package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName
import shop.itbug.fluttercheckversionx.util.DateUtils
import shop.itbug.fluttercheckversionx.util.YamlExtends
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel


class DartPackageCheckActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        DartPackageCheckService.getInstance(project).start()
    }
}

data class MyDartPackage(
    var packageName: String,
    val element: YAMLKeyValueImpl,
    val detail: DartPluginVersionName,
    val versionElement: YAMLPlainTextImpl,
    val error: String? = null,
    val service: DartPackageCheckService? = null
) {
    /**
     * 通过 api来向 pub加载插件的数据
     */
    fun getDetailApi(): Pair<MyDartPackage, PubVersionDataModel?> {
        try {
            val r = PubService.callPluginDetails(packageName)
            return Pair(this, r)
        } catch (e: Exception) {
            return Pair(this.copy(error = e.localizedMessage), null)
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

/**
 * 项目包检测的服务类
 */
@Service(Service.Level.PROJECT)
class DartPackageCheckService(val project: Project) {
    private val ignoreManager = DartPluginIgnoreConfig.getInstance(project)
    var details: MutableList<Pair<MyDartPackage, PubVersionDataModel?>> = mutableListOf()///从服务器获取的数据


    /**
     * 读取项目的包文件,pubspec.yaml
     */
    private fun getPubspecFile(): YAMLFile? {
        val projectDir = project.guessProjectDir() ?: return null
        val pubspecFile = projectDir.findChild("pubspec.yaml") ?: return null
        val file = ApplicationManager.getApplication().runReadAction(Computable<PsiFile?> {
            PsiManager.getInstance(project).findFile(pubspecFile)
        })
        return file as YAMLFile
    }

    /**
     * 解析插件列表,不要在这里执行耗时的操作
     */
    private fun getPackageInfos(): List<MyDartPackage> {
        val list = mutableListOf<MyDartPackage>()
        val pubspecFile = getPubspecFile() ?: return emptyList()
        val dependenciesElement =
            runReadAction { YAMLUtil.getQualifiedKeyInFile(pubspecFile, "dependencies") } ?: return emptyList()
        val block = dependenciesElement.value as? YAMLBlockMappingImpl ?: return emptyList()
        val packageElements = runReadAction { PsiTreeUtil.findChildrenOfType(block, YAMLKeyValueImpl::class.java) }

        packageElements.forEach { element ->
            runReadAction {
                val ext = YamlExtends(element)
                val info: DartPluginVersionName? = ext.getDartPluginNameAndVersion()
                if (info != null) {
                    if (ignoreManager.isIg(info.name).not() && !ext.isSpecifyVersion()) {
                        val lastTextElement = PsiTreeUtil.findChildOfType(element, YAMLPlainTextImpl::class.java)
                        if (lastTextElement != null) {
                            val item = MyDartPackage(
                                info.name, element as YAMLKeyValueImpl, info, lastTextElement, service = this
                            )
                            list.add(item)
                        }
                    }
                }
            }
        }
        return list
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
    }


    /**
     * 判断是否有新版本
     */
    fun hasNew(item: Pair<MyDartPackage, PubVersionDataModel?>): Boolean {
        val packageName = item.first.getDartPluginVersionName()
        val second = item.second ?: return false
        return second.getLastVersionText(packageName) != null
    }


    /**
     * 重新索引
     */
    fun resetIndex() {
        details = mutableListOf()
        start()
    }


    fun getTableRows(): List<Array<String>> {
        return details.map {
            val date = it.second?.lastVersionUpdateTimeString ?: ""
            val timeAgo = if (date.isBlank()) "-" else DateUtils.timeAgo(date)
            arrayOf(
                it.first.packageName,
                it.first.detail.version,
                it.second?.latest?.version ?: "-",
                "$date ($timeAgo)"
            )
        }
    }

    companion object {
        /**
         * 读取项目包操作模块的实例
         */
        fun getInstance(project: Project): DartPackageCheckService {
            return project.service<DartPackageCheckService>()
        }
    }
}


///dart包详情面板
class DartPluginsPanel(val project: Project) : DialogWrapper(project) {

    private val table = JBTable()

    init {
        super.init()
        changeTableData()
    }


    override fun getSize(): Dimension {
        return preferredSize
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(1000, 600)
    }

    override fun createCenterPanel(): JComponent {
        return BorderLayoutPanel().apply {
            addToCenter(JBScrollPane(table).apply {
                this.preferredSize = this@DartPluginsPanel.preferredSize
            })
        }
    }

    /**
     * 显示数据
     */
    private fun changeTableData() {
        val packageService = DartPackageCheckService.getInstance(project)
        table.model =
            DefaultTableModel(
                arrayOf("Name", "Current Version(in file)", "Last Version(Pub.dev)", "Last Update"),
                0
            ).apply {
                packageService.getTableRows().forEach { arr ->
                    this.addRow(arr)
                }
            }
        table.columnModel.let {
            it.getColumn(0).minWidth = 250
            it.getColumn(1).minWidth = 200
            it.getColumn(2).minWidth = 200
            it.getColumn(3).minWidth = 300
        }
    }

    companion object {
        /**
         * 展示数据
         */
        fun showInCenter(project: Project) {
            DartPluginsPanel(project).show()
        }
    }

}