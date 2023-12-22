package shop.itbug.fluttercheckversionx.window.sp

import com.alibaba.fastjson2.JSONObject
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class SpWindow(project: Project, private val toolWindow: ToolWindow) : BorderLayoutPanel(),
    DioApiService.NativeMessageProcessing {


    val button = JButton("获取所有Key")
    private val left = SpWindowLeft()
    private val right = SpWindowRight(project)


    val content = OnePixelSplitter().apply {
        firstComponent = JBScrollPane(left).apply { border = null }
        secondComponent = right
        this.splitterProportionKey = "sp-window-key"
        border = BorderFactory.createEmptyBorder()
    }

    init {
        register()
        getAllKeys()
        addToTop(createToolbar().component)
        addToCenter(content)
    }

    private fun getAllKeys() {
        DioApiService.INSTANCESupplier.get().sendByMap(mutableMapOf("action" to "SP_KEY"))
    }


    private fun createToolbar(): ActionToolbar {
        return ActionManager.getInstance().createActionToolbar("sp tool bar", DefaultActionGroup().apply {
            this.add(ActionManager.getInstance().getAction("shop.itbug.fluttercheckversionx.window.sp.SpRefreshAction"))
        }, true).apply {
            this.targetComponent = toolWindow.component
        }
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: JSONObject?, aio: AioSession?) {
        if (jsonObject != null) {
            val type = jsonObject.getString("type")
            if (type == SpManager.KEYS || type == SpManager.VALUE_GET) {
                SpManager(nativeMessage).handle()
            }
        }

    }

}


///刷新sp keys操作
class SpRefreshAction : DumbAwareAction(AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
        DioApiService.INSTANCESupplier.get().sendByMap(mutableMapOf("action" to "SP_KEY"))
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}

///数据展示区域
class SpWindowRight(project: Project) : BorderLayoutPanel() {
    private var jsonView: JsonValueRender = JsonValueRender(p = project)

    init {
        addToCenter(JBScrollPane(jsonView).apply {
            this.border = BorderFactory.createEmptyBorder()
        })
        SpManagerListen.listen(null) {
            jsonView.changeValue(it?.value)
        }
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
    }


}


/// keys 列表
class SpWindowLeft : JBList<String>(), ListSelectionListener {
    init {
        SpManagerListen.listen(modelHandel = {
            this.model = DefaultListModel<String>().apply {
                this.addAll(it.keys)
            }
        }, valueHandle = null)
        addListSelectionListener(this)
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
    }


    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            DioApiService.INSTANCESupplier.get()
                .sendByMap(mutableMapOf("action" to "SP_GET_VALUE", "data" to selectedValue))
        }
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(200, -1)
    }

    override fun getMinimumSize(): Dimension {
        return preferredSize
    }

}

