package shop.itbug.fluttercheckversionx.window.logger

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import java.awt.Component
import java.time.LocalDateTime
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

data class MyLogKey(val key: String)
data class MyLogInfo(val message: String, val time: LocalDateTime? = LocalDateTime.now(), val key: MyLogKey)
object LogKeys {
    val ping = MyLogKey(key = "Socket Ping")
}

///日志窗口
class LoggerWindow(val project: Project) : BorderLayoutPanel(), ListSelectionListener, Disposable {


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
        MyLoggerEvent.listen(this) {
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

    override fun dispose() {
        println("log window disposed")
    }


}

private const val borderSize = 12

class LogRenderUI : ListCellRenderer<MyLogInfo> {

    override fun getListCellRendererComponent(
        list: JList<out MyLogInfo>?,
        value: MyLogInfo?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return panel {
            row {
                label(value?.message ?: "")
            }
        }.withBorder(BorderFactory.createEmptyBorder(borderSize / 2, borderSize, borderSize / 2, borderSize)).apply {
            background = if (isSelected) UIUtil.getListBackground(true, false) else UIUtil.getPanelBackground()
        }
    }
}

class MyLogKeysRenderUi : ListCellRenderer<MyLogKey> {

    override fun getListCellRendererComponent(
        list: JList<out MyLogKey>?,
        value: MyLogKey?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return panel {
            row {
                label(value?.key ?: "")
            }
        }.withBorder(BorderFactory.createEmptyBorder(borderSize / 2, borderSize, borderSize / 2, borderSize)).apply {
            background = if (isSelected) UIUtil.getListBackground(true, false) else UIUtil.getPanelBackground()
        }
    }
}