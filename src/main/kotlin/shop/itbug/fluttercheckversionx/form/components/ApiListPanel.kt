package shop.itbug.fluttercheckversionx.form.components

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.AsyncDataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.bus.DioWindowApiSearchBus
import shop.itbug.fluttercheckversionx.bus.DioWindowCleanRequests
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.form.socket.MyCustomItemRender
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.projectClosed
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * api列表
 */
class ApiListPanel(val project: Project) : JBList<Request>(), ListSelectionListener {

    private val appService = service<AppService>()
    private fun listModel(): DefaultListModel<Request> = model as DefaultListModel

    init {
        model = DefaultListModel()
        cellRenderer = MyCustomItemRender()
        setNewApiInChangeList()
        setApiListEmptyText()
        addListSelectionListener(this)
        addListening()
        addRightPopupMenuClick()
        DioWindowApiSearchBus.listing { doSearch(it) }
        DioWindowCleanRequests.listening { listModel().clear() }
    }

    /**
     * 右键菜单监听
     */
    private fun addRightPopupMenuClick() {
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (e != null && SwingUtilities.isRightMouseButton(e)) {
                    val index = this@ApiListPanel.locationToIndex(e.point)
                    selectedIndex = index
                    SwingUtilities.invokeLater {
                        createPopupMenu().show(RelativePoint(Point(e.locationOnScreen)))
                    }
                }
            }
        })
    }


    /**
     * 查看菜单
     */
    fun createPopupMenu(): ListPopup {
        return JBPopupFactory.getInstance()
            .createActionGroupPopup(PluginBundle.get("menu"), myActionGroup, myDataContext, true, { dispose() }, 10)

    }

    private val myActionGroup: ActionGroup
        get() = ActionManager.getInstance().getAction("dio-window-view-params") as ActionGroup


    private val myDataContext: AsyncDataContext = object : AsyncDataContext {
        override fun getData(dataId: String): Any? {
            if (dataId == SELECT_KEY) {
                return this@ApiListPanel.selectedValue
            } else if (dataId == PROJECT_KEY) {
                return project
            }
            return null
        }
    }

    /**
     * 菜单销毁回调
     */
    private fun dispose() {

    }


    /**
     * 搜索接口
     */
    private fun doSearch(keyword: String) {
        val allRequest = appService.getAllRequest()
        val results = allRequest.filter { it.url?.uppercase()?.contains(keyword.uppercase()) ?: false }
        if (results.isNotEmpty()) {
            listModel().apply {
                clear()
                addAll(results)
            }
        } else {
            listModel().apply {
                clear()
                addAll(appService.getCurrentProjectAllRequest())
            }
        }
    }

    /**
     * 项目切换监听
     */
    private fun addListening() {
        SwingUtilities.invokeLater {
            val runnable = Runnable { projectChange() }
            service<AppService>().addListening(runnable)
            project.projectClosed { service<AppService>().removeListening(runnable) }
        }
    }

    /**
     * 项目被切换事件
     */
    private fun projectChange() {
        val appService = service<AppService>()
        val projectName = appService.currentSelectName.get()
        projectName?.let {
            val apis = appService.getRequestsWithProjectName(projectName)
            changeApis(apis)
        }
    }

    /**
     * 更新api列表
     */
    private fun changeApis(apis: List<Request>) {
        listModel().apply {
            clear()
            addAll(apis)
        }
    }

    /**
     * 监听到api进入,更新模型
     */
    private fun setNewApiInChangeList() {
        val appService = service<AppService>()
        SocketMessageBus.listening {
            val currentProjectName = appService.currentSelectName.get()
            //如果没有选中项目, 或者当前选中项目等于进入api的项目,才被加进列表中
            if (currentProjectName == null || currentProjectName == it.projectName) {
                listModel().addElement(it)
            }
        }
    }


    ///添加帮助性文档
    private fun setApiListEmptyText() {
        setEmptyText(PluginBundle.get("empty"))
        emptyText.apply {
            appendLine(
                PluginBundle.get("help"), SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    JBUI.CurrentTheme.Link.Foreground.ENABLED
                )
            ) {
                BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/dio.md")
            }
            appendText(PluginBundle.get("split.symbol"))
            appendText(
                PluginBundle.get("reward"),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED, JBUI.CurrentTheme.Link.Foreground.ENABLED)
            ) {
                RewardDialog(project).show()
            }
            appendText(PluginBundle.get("split.symbol"))
            appendText(
                PluginBundle.get("bugs"),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED, JBUI.CurrentTheme.Link.Foreground.ENABLED)
            ) {
                BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
            }
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null) {
            if (!e.valueIsAdjusting && selectedValue != null) {
                FlutterApiClickBus.fire(selectedValue)
            }
        }
    }


    companion object {
        const val SELECT_KEY = "select-api"
        const val PROJECT_KEY = "project-"
    }

}


