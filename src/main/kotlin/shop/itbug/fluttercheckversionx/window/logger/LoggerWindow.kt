package shop.itbug.fluttercheckversionx.window.logger

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.actions.context.SiteDocument
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import java.time.LocalDateTime
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JList
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

enum class MyLogIconType {
    None,
    Error,
    Info,
    Success,
    Warning;

    fun getIcon(): Icon? {
        return when (this) {
            None -> null
            Error -> AllIcons.General.Error
            Info -> AllIcons.General.Information
            Success -> AllIcons.Status.Success
            Warning -> AllIcons.General.Warning
        }
    }
}

data class MyLogKey(val key: String)
data class MyLogInfo(
    val message: Any?,
    val time: LocalDateTime? = LocalDateTime.now(),
    val key: MyLogKey,
    val subTitle: String?,
    val logType: Int?
)


///日志窗口
class LoggerWindow(val project: Project) : BorderLayoutPanel(), ListSelectionListener, Disposable,
    DioApiService.HandleFlutterApiModel {


    ///keys 窗口
    private val keysPanel = MyLogPanel()

    private val jsonRender = JsonValueRender(project)


    ///分割窗口
    private val sp = OnePixelSplitter().apply {
        firstComponent = JBScrollPane(keysPanel).apply {
            this.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 0)
        }
        secondComponent = JBScrollPane(jsonRender).apply {
            this.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 0)
        }
        splitterProportionKey = "logger-panel-key"
    }

    init {
        register()
        addToCenter(sp)
        MyLoggerEvent.listen(this) {
            addLog(it)
        }
        initUi()
    }


    private fun getModel() = keysPanel.model as DefaultListModel

    private fun addLog(log: MyLogInfo) {
        getModel().add(0, log)
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        super.handleFlutterAppMessage(nativeMessage, jsonObject, aio)
        val type = jsonObject?.get("type") as? String ?: return
        if (type == "customJsonLog") {
            val value = jsonObject["jsonDataString"] as? String ?: return
            val obj = DioApiService.getInstance().gson.fromJson(value, Map::class.java)
            val title = obj["title"] as? String ?: return
            val subTitle = obj["subTitle"] as? String
            val data = obj["data"]
            val logType = ((obj["logType"] as? Long) ?: 0).toInt()
            MyLoggerEvent.fire(MyLogInfo(message = data, key = MyLogKey(title), subTitle = subTitle, logType = logType))

        }


    }


    private fun initUi() {
        keysPanel.cellRenderer = MyLogKeysRenderUi()
        keysPanel.addListSelectionListener(this)
        keysPanel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }


    ///刷新右侧面板的日志列表
    private fun refreshSelectLogs() {
        val selectKey = keysPanel.selectedValue
        jsonRender.changeValue(selectKey.message)

    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && !project.isDisposed) {
            if (!e.valueIsAdjusting && keysPanel.selectedValue != null) {
                refreshSelectLogs()
            }
        }
    }

    override fun dispose() {
        println("log window disposed")
        removeMessageProcess()
        keysPanel.removeListSelectionListener(this)
    }


}


class MyLogKeysRenderUi : ColoredListCellRenderer<MyLogInfo>() {
    override fun customizeCellRenderer(
        p0: JList<out MyLogInfo?>,
        p1: MyLogInfo?,
        p2: Int,
        p3: Boolean,
        p4: Boolean
    ) {
        p1?.let {

            it.logType?.let { logType ->
                icon = MyLogIconType.entries[logType].getIcon()
            }

            append(it.key.key)
            it.subTitle?.let { subTitle ->
                appendTextPadding(12)
                append(subTitle, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES, 12, 1)
            }
        }
    }

}


private class MyLogPanel : JBList<MyLogInfo>(DefaultListModel()), UiDataProvider {
    val actionGroup = DefaultActionGroup()
    private val removeAction =
        object : DumbAwareAction(PluginBundle.get("delete_base_text"), "Remove item", AllIcons.General.Delete) {
            override fun actionPerformed(p0: AnActionEvent) {
                this@MyLogPanel.getListModel().removeElement(this@MyLogPanel.selectedValue)
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = this@MyLogPanel.selectedValue != null
                super.update(e)
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }
        }

    init {
        actionGroup.add(removeAction)
        ListUiUtil.Selection.installSelectionOnRightClick(this)
        ListUiUtil.Selection.installSelectionOnFocus(this)
        TreeUIHelper.getInstance().installListSpeedSearch(this) { o -> o.key.key }
        PopupHandler.installPopupMenu(this, actionGroup, "FlutterX.Log.Window.Keys")
        setEmptyText(PluginBundle.get("empty"))
        emptyText.appendLine("")
        emptyText.appendLine(
            PluginBundle.get("help"), SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.Foreground.ENABLED
            )
        ) {
            SiteDocument.Log.open()
        }
    }

    override fun uiDataSnapshot(sink: DataSink) {

    }

    private fun getListModel() = model as DefaultListModel
}
