package shop.itbug.fluttercheckversionx.form.components

import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.bus.DioWindowApiSearchBus
import shop.itbug.fluttercheckversionx.bus.DioWindowCleanRequests
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.config.DioSettingChangeEvent
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.form.socket.MyCustomItemRender
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.listeners.FlutterProjectChangeEvent
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.Util
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener


/**
 * api列表
 */
class ApiListPanel(val project: Project) : JBList<Request>(), ListSelectionListener, DataProvider,
    DioApiService.HandleFlutterApiModel, FlutterProjectChangeEvent {

    private val appService = service<AppService>()
    private fun listModel(): ItemModel = model as ItemModel


    fun createPopupMenu(): ListPopup {

        return JBPopupFactory.getInstance().createActionGroupPopup(
            null, myActionGroup, DataManager.getInstance().getDataContext(this), true, { }, 10
        )

    }


    init {
        register()
        connectFlutterProjectChangeEvent()
        model = ItemModel(mutableListOf())
        cellRenderer = MyCustomItemRender()
        setNewApiInChangeList()
        setApiListEmptyText()
        addListSelectionListener(this)
        addRightPopupMenuClick()
        border = null
        DioWindowApiSearchBus.listing { doSearch(it) }
        DioWindowCleanRequests.listening { listModel().clear() }
        DioSettingChangeEvent.listen { _, _ ->
            refreshUi()
        }
        SwingUtilities.invokeLater {
            appService.refreshProjectRequest(project)
        }
    }


    override fun setDataProvider(provider: DataProvider) {
        super.setDataProvider { s ->
            {
                if (s == SELECT_KEY) {
                    this.selectedValue
                }
            }
        }
    }


    ///重新构建一下 UI
    private fun refreshUi() {
        model = ItemModel(listModel().list)
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
                    appService.currentSelectRequest = selectedValue
                    SwingUtilities.invokeLater {
                        createPopupMenu().show(RelativePoint(Point(e.locationOnScreen)))
                    }
                }
            }
        })
    }


    private val myActionGroup: ActionGroup
        get() = ActionManager.getInstance().getAction("dio-window-view-params") as ActionGroup


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
     * 更新api列表
     */
    private fun changeApisModel(apis: MutableList<Request>) {
        model = ItemModel(apis)
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
            appendLine("")
            appendLine(
                PluginBundle.get("help"), SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.Foreground.ENABLED
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
            appendLine("IP:${
                Util.resolveLocalAddresses()
                    .filter { it.hostAddress.split('.').size == 4 && it.hostAddress.split(".")[2] != "0" }
                    .map { it.hostAddress }
            }", SimpleTextAttributes.GRAYED_ATTRIBUTES) {}
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && !project.isDisposed) {
            if (!e.valueIsAdjusting && selectedValue != null) {
                FlutterApiClickBus.fire(selectedValue)
            }
        }
    }


    companion object {
        const val SELECT_KEY = "select-api"
        const val PANEL = "panel"
    }

    override fun getData(p0: String): Any? {
        if (p0 == PANEL) {
            return this
        }
        return null
    }


    override fun handleModel(model: ProjectSocketService.SocketResponseModel) {

        changeApisModel(appService.getCurrentProjectAllRequest().toMutableList())
        super.handleModel(model)
    }


    private inner class ItemModel(val list: MutableList<Request>) : DefaultListModel<Request>() {
        init {
            addAll(list)
        }
    }

    //更新项目
    override fun changeProject(projectName: String, p: Project?) {
        changeApisModel(appService.getCurrentProjectAllRequest().toMutableList())
    }


}

fun JComponent.createDecorator(block: (dec: ToolbarDecorator) -> ToolbarDecorator): JPanel {
    var r = ToolbarDecorator.createDecorator(this).setPanelBorder(null).disableUpDownActions().disableRemoveAction()

    r = block.invoke(r)
    return r.createPanel().apply {
        border = null
    }
}

