package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import shop.itbug.fluttercheckversionx.model.*
import shop.itbug.fluttercheckversionx.util.ApiService
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import javax.swing.DefaultListModel
import javax.swing.JList


private typealias MyListPairModel = Pair<FlutterPluginElementModel, PubVersionDataModel>

///检测版本小窗口
class AllPluginsCheckVersion(val project: Project) : BorderLayoutPanel() {


    private val log = LoggerFactory.getLogger(AllPluginsCheckVersion::class.java)

    //插件列表
    private var plugins: MutableMap<FlutterPluginType, List<FlutterPluginElementModel>> =
        MyPsiElementUtil.getAllFlutters(project)

    //展示列表组件
    private val listView = JBList<MyListPairModel>()


    init {
        addToCenter(JBScrollPane(listView))
        listView.cellRenderer = PluginListCellRender()
        ApplicationManager.getApplication().invokeLater {
            initRequest()
        }
    }

    /**
     * 开始检测插件新版本
     * 需要访问网络
     */
    private fun initRequest() {

        log.info("开始检测插件版本")
        ///全部的版本列表
        val result: List<PubVersionDataModel?> = runBlocking(Dispatchers.IO) {
            val checkTasks = plugins.values.flatten()
                .map { plugin -> return@map async { return@async ApiService.getPluginDetail(plugin.name) } }
            return@runBlocking checkTasks.awaitAll()
        }


        log.info("全部检测插件完成:${result.size}")
        val hasNewVersionPlugins = mutableListOf<Pair<FlutterPluginElementModel, PubVersionDataModel>>()
        plugins.values.flatten().forEach { plugin ->
            val findVersionInfoModel = result.find { it?.name == plugin.name }
            findVersionInfoModel?.let { info: PubVersionDataModel ->
                if (info.hasNewVersion(plugin.dartPluginModel)) {
                    hasNewVersionPlugins.add(Pair(plugin, info))
                }
            }
        }



        ApplicationManager.getApplication().invokeLater {
            if (hasNewVersionPlugins.isNotEmpty()) {
                listView.model = PluginListModel().apply {
                    clear()
                    addAll(hasNewVersionPlugins)
                }
            }
        }


    }


}


private class PluginListModel : DefaultListModel<MyListPairModel>()

private class PluginListCellRender : ColoredListCellRenderer<MyListPairModel>() {
    override fun customizeCellRenderer(
        list: JList<out MyListPairModel>,
        value: MyListPairModel?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {

        if (value != null) {
            append(value.second.name)
            value.second.getLastVersionText(value.first.dartPluginModel)?.let { append(":$it") }
        }

    }


}