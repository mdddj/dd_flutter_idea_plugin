package shop.itbug.fluttercheckversionx.form.components

import com.google.gson.Gson
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.PopupHandler
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import shop.itbug.fluttercheckversionx.actions.context.SiteDocument
import shop.itbug.fluttercheckversionx.bus.DioWindowApiSearchBus
import shop.itbug.fluttercheckversionx.bus.DioWindowCleanRequests
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DioSettingChangeEvent
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.dsl.formatUrl
import shop.itbug.fluttercheckversionx.form.socket.MyCustomItemRender
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.listeners.FlutterProjectChangeEvent
import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.tools.MyToolWindowTools
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import shop.itbug.fluttercheckversionx.util.Util
import shop.itbug.fluttercheckversionx.window.logger.LogKeys
import shop.itbug.fluttercheckversionx.window.logger.MyLogInfo
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities
import javax.swing.event.HyperlinkEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener


/**
 * api列表
 */
class ApiListPanel(val project: Project) : JBList<Request>(), ListSelectionListener, UiDataProvider,
    DioApiService.HandleFlutterApiModel, FlutterProjectChangeEvent, Disposable {

    private val appService = AppService.getInstance()
    private fun listModel(): ItemModel = model as ItemModel


    init {
        register()
        connectFlutterProjectChangeEvent(this)
        model = ItemModel(mutableListOf())
        cellRenderer = MyCustomItemRender()
        setApiListEmptyText()
        addListSelectionListener(this)
        border = emptyBorder()
        DioWindowApiSearchBus.listing(this) { doSearch(it) }
        DioWindowCleanRequests.listening(this) { listModel().clear() }
        DioSettingChangeEvent.listen(this) { _, _ ->
            refreshUi()
        }
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


    ///重新构建一下 UI
    private fun refreshUi() {
        model = ItemModel(listModel().list)
    }


    private val myActionGroup: ActionGroup
        get() = ActionManager.getInstance().getAction("dio-window-view-params") as ActionGroup


    /**
     * 搜索接口
     */
    private fun doSearch(keyword: String) {
        val allRequest = appService.getAllRequest()
        val results = allRequest.filter { it.url.uppercase().contains(keyword.uppercase()) }
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
        println("change api...")
        val isReverse = DioListingUiConfig.setting.isReverseApi
        if (isReverse.not()) {
            model = ItemModel(apis)
        } else {
            apis.reverse()
            model = ItemModel(apis)
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

    override fun handleModel(model: ProjectSocketService.SocketResponseModel) {
        if (project.isDisposed) return
        try {
            MyLoggerEvent.fire(MyLogInfo(message = Gson().toJson(model), key = LogKeys.dioLog))
        } catch (_: Exception) {
        }
        showNewApiTips(model)
        changeApisModel(appService.getCurrentProjectAllRequest().toMutableList())
        super.handleModel(model)
    }


    private fun showNewApiTips(req: Request) {

        val config = PluginStateService.getInstance().state
        val manager = ToolWindowManager.getInstance(project)
        val windowId = MyToolWindowTools.windowId
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

    private inner class ItemModel(val list: MutableList<Request>) : DefaultListModel<Request>() {
        init {
            addAll(list)
        }
    }

    //更新项目
    override fun changeProject(projectName: String, project: Project?) {
        changeApisModel(appService.getCurrentProjectAllRequest().toMutableList())
    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[SELECT_ITEM] = selectedValue
        sink[PANEL_DATA_KEY] = this
    }

    override fun dispose() {
        println("api list panel dispose....")
    }


}
