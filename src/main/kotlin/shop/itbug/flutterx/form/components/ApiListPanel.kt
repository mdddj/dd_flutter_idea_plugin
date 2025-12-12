package shop.itbug.flutterx.form.components

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.PopupHandler
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import shop.itbug.flutterx.actions.context.SiteDocument
import shop.itbug.flutterx.bus.DioWindowApiSearchBus
import shop.itbug.flutterx.bus.DioWindowCleanRequests
import shop.itbug.flutterx.bus.FlutterApiClickBus
import shop.itbug.flutterx.config.*
import shop.itbug.flutterx.dialog.RewardDialog
import shop.itbug.flutterx.dsl.formatUrl
import shop.itbug.flutterx.form.socket.MyCustomItemRender
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.listeners.FlutterProjectChangeEvent
import shop.itbug.flutterx.model.getHtmlPrefix
import shop.itbug.flutterx.model.getMessageType
import shop.itbug.flutterx.services.PluginStateService
import shop.itbug.flutterx.socket.Request
import shop.itbug.flutterx.socket.service.AppService
import shop.itbug.flutterx.socket.service.DioApiService
import shop.itbug.flutterx.tools.MyToolWindowTools
import shop.itbug.flutterx.tools.emptyBorder
import shop.itbug.flutterx.util.Util
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.HyperlinkEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener


/**
 * api列表
 */
class ApiListPanel(val project: Project) : JBList<Request>(ItemModel(mutableListOf())), ListSelectionListener,
    UiDataProvider, DioApiService.HandleFlutterApiModel, FlutterProjectChangeEvent, Disposable,
    DioSettingChangeEventChangeFun {

    private val appService = AppService.getInstance()
    private val configChangeListeners = mutableListOf<OnChangeConfigListen>()

    init {
        register()
        connectFlutterProjectChangeEvent(this)
        cellRenderer = MyCustomItemRender()
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setApiListEmptyText()
        addListSelectionListener(this)
        border = emptyBorder()
        DioWindowApiSearchBus.listing(this) { doSearch(it) }
        DioWindowCleanRequests.listening(this) { getListModel().clear() }
        DioSettingChangeEvent.listen(this, this)
        SwingUtilities.invokeLater {
            appService.refreshProjectRequest(project)
        }
        ListUiUtil.Selection.installSelectionOnFocus(this)
        ListUiUtil.Selection.installSelectionOnRightClick(this)
        PopupHandler.installPopupMenu(this, myActionGroup, "Dio Menu")
        TreeUIHelper.getInstance().installListSpeedSearch(this) { o ->
            o.formatUrl(
                DioListingUiConfig.setting
            )
        }
    }


    ///获取列表模型
    fun getListModel() = model as ItemModel


    private val myActionGroup: ActionGroup
        get() = ActionManager.getInstance().getAction("dio-window-view-params") as ActionGroup


    /**
     * 搜索接口
     */
    private fun doSearch(keyword: String) {
        val allRequest = appService.getAllRequest()
        val results = allRequest.filter { it.url.uppercase().contains(keyword.uppercase()) }
        if (results.isNotEmpty()) {
            changeApisModel(results)
        } else {
            changeApisModel(appService.getCurrentProjectAllRequest())
        }
    }


    //刷新UI
    private fun refreshListWithConfig() {
        changeApisModel()
    }


    //更新api列表显示
    private fun changeApisModel(apis: List<Request> = appService.getCurrentProjectAllRequest()) {
        if (apis.isNotEmpty()) {
            ApplicationManager.getApplication().invokeLater {
                val isReverse = DioListingUiConfig.setting.isReverseApi
                if (isReverse.not()) {
                    getListModel().changeList(apis)
                } else {
                    val mList = apis.toMutableList().reversed()
                    getListModel().changeList(mList)
                }
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
            if (PluginConfig.getState(project).showRewardAction) {
                appendText(
                    PluginBundle.get("reward"),
                    SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED, JBUI.CurrentTheme.Link.Foreground.ENABLED)
                ) {
                    RewardDialog(project).show()
                }
            }
            appendText(PluginBundle.get("split.symbol"))
            appendText(
                PluginBundle.get("bugs"),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED, JBUI.CurrentTheme.Link.Foreground.ENABLED)
            ) {
                BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
            }
            appendText(PluginBundle.get("split.symbol"))
            appendText(
                PluginBundle.get("document"), SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.Foreground.ENABLED
                )
            ) {
                BrowserUtil.open(SiteDocument.Dio.url)
            }
            appendLine(
                "IP:${
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
        val PANEL_DATA_KEY = DataKey.create<ApiListPanel>("panel")
        val SELECT_ITEM = DataKey.create<Request>("select-api")
    }

    override fun handleModel(model: Request) {
        if (project.isDisposed) return
        if (appService.getCurrentSelectProjectName() == model.projectName) {
            showNewApiTips(model)
            getListModel().addItem(model, DioListingUiConfig.setting.isReverseApi)
        }
        super.handleModel(model)
    }


    //弹窗新接口提示
    private fun showNewApiTips(req: Request) {
        val config = PluginStateService.getInstance().state
        val manager = ToolWindowManager.getInstance(project)
        val windowId = MyToolWindowTools.WINDOW_ID
        if (config?.apiInToolwindowTop == true && manager.canShowNotification(windowId)) {
            ///在窗口弹出一个api提醒
            manager.notifyByBalloon(
                windowId,
                req.getMessageType(),
                "<html>${req.getHtmlPrefix()} <a href='${req.url}'>${req.url}</a></html>",
                MyIcons.flutter
            ) { e: HyperlinkEvent? ->
                e?.let {
                    println("event: ${e.eventType}")
                    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                        e.url?.let { url ->
                            BrowserUtil.browse(url)
                        }
                    }
                }
            }
        }
    }


    //更新项目
    override fun changeProject(projectName: String, project: Project?) {
        changeApisModel()
    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[SELECT_ITEM] = selectedValue
        sink[PANEL_DATA_KEY] = this
    }

    override fun dispose() {
        configChangeListeners.clear()
    }

    override fun invoke(
        p1: DoxListeningSetting, p2: DoxListeningSetting
    ) {
        for (listen in configChangeListeners) {
            listen.listenChanged(p1, p2)
        }
        refreshListWithConfig()
    }

    fun addOnChangeConfigListen(obj: OnChangeConfigListen) {
        configChangeListeners.add(obj)
    }

    fun removeOnChangeConfigListen(obj: OnChangeConfigListen) {
        configChangeListeners.remove(obj)
    }

    interface OnChangeConfigListen {
        fun listenChanged(p1: DoxListeningSetting, p2: DoxListeningSetting)
    }
}

//模型
class ItemModel(val list: List<Request>) : DefaultListModel<Request>() {

    init {
        addAll(list)
    }

    //添加项目
    fun addItem(newItem: Request, isReverse: Boolean) {
        if (isReverse) {
            add(0, newItem)
        } else {
            add(size, newItem)
        }
    }

    //切换显示列表
    fun changeList(newItems: List<Request>) {
        removeAllElements()
        addAll(newItems)
    }
}