package shop.itbug.fluttercheckversionx.window.logger

import cn.hutool.core.date.DateTime
import cn.hutool.core.date.DateUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

data class MyLogKey(val key: String)
data class MyLogInfo(val message: String, val time: DateTime? = DateUtil.date(), val key: MyLogKey)
object LogKeys {
    val dioLog = MyLogKey(key = "Dio Listen")
    val ping = MyLogKey(key = "Socket Ping")
    val checkPlugin = MyLogKey(key = "check plugins")
}

///日志窗口
class LoggerWindow(val project: Project) : BorderLayoutPanel(), ListSelectionListener {


    ///日志列表
    private val loggers = mutableListOf<MyLogInfo>()


    ///keys 窗口
    private val keysPanel = JBList<MyLogKey>()

    ///values 窗口
    private val logPanel = JBList<MyLogInfo>()


    private val loggerGroup: Map<MyLogKey, List<MyLogInfo>> get() = loggers.groupBy { it.key }

    ///分割窗口
    private val sp = OnePixelSplitter().apply {
        firstComponent = JBScrollPane(keysPanel).apply {
            this.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 0)
        }
        secondComponent = JBScrollPane(logPanel).apply {
            this.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 0)
        }
        splitterProportionKey = "logger-panel-key"
    }

    init {
        addToCenter(sp)
        MyLoggerEvent.listen {
            addLog(it)
        }
        initUi()
    }


    private fun addLog(log: MyLogInfo) {
        loggers.add(log)
        reSetPanel()
    }


    private fun initUi() {
        keysPanel.cellRenderer = MyLogKeysRenderUi()
        logPanel.cellRenderer = LogRenderUI()

        keysPanel.addListSelectionListener(this)
    }


    private fun reSetPanel() {

        val preSelect = keysPanel.selectedValue

        keysPanel.model = DefaultListModel<MyLogKey>().apply {
            addAll(loggerGroup.keys)
        }
        if (preSelect != null) {
            keysPanel.setSelectedValue(preSelect, true)
        }
        refreshSelectLogs()

    }


    ///刷新右侧面板的日志列表
    private fun refreshSelectLogs() {
        val selectKey = keysPanel.selectedValue
        if (selectKey != null) {
            val logs = loggerGroup[selectKey]
            if (logs?.isNotEmpty() == true) {
                logPanel.model = DefaultListModel<MyLogInfo>().apply {
                    addAll(logs)
                }
            }

        }

    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && !project.isDisposed) {
            if (!e.valueIsAdjusting && keysPanel.selectedValue != null) {
                refreshSelectLogs()
            }
        }
    }


}


class LogRenderUI : ColoredListCellRenderer<MyLogInfo>() {
    override fun customizeCellRenderer(p0: JList<out MyLogInfo>, p1: MyLogInfo?, p2: Int, p3: Boolean, p4: Boolean) {
        p1?.let {
            append(it.message)
        }
    }
}

class MyLogKeysRenderUi : ColoredListCellRenderer<MyLogKey>() {
    override fun customizeCellRenderer(p0: JList<out MyLogKey>, p1: MyLogKey?, p2: Int, p3: Boolean, p4: Boolean) {
        p1?.let {
            append(it.key)

        }
    }
}