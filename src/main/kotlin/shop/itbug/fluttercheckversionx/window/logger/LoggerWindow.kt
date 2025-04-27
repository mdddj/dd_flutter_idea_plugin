package shop.itbug.fluttercheckversionx.window.logger

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import java.time.LocalDateTime
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

data class MyLogKey(val key: String)
data class MyLogInfo(val message: Any?, val time: LocalDateTime? = LocalDateTime.now(), val key: MyLogKey)


///日志窗口
class LoggerWindow(val project: Project) : BorderLayoutPanel(), ListSelectionListener, Disposable,
    DioApiService.HandleFlutterApiModel {


    ///keys 窗口
    private val keysPanel = JBList(DefaultListModel<MyLogInfo>())

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
            val data = obj["data"]
            MyLoggerEvent.fire(MyLogInfo(message = data, key = MyLogKey(title)))

        }


    }


    private fun initUi() {
        keysPanel.cellRenderer = MyLogKeysRenderUi()

        keysPanel.addListSelectionListener(this)
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
            append(it.key.key)
        }
    }


}