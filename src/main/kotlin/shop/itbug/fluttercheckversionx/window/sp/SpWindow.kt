package shop.itbug.fluttercheckversionx.window.sp

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.actions.context.HelpContextAction
import shop.itbug.fluttercheckversionx.actions.context.SiteDocument
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class SpWindow(project: Project, private val toolWindow: ToolWindow) : BorderLayoutPanel(),
    DioApiService.NativeMessageProcessing {


    private val left = SpWindowLeft()
    private val right = SpWindowRight(project)
    private val emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0)


    init {
        register()
        getAllKeys()
        addToTop(createToolbar().component)
        addToCenter(OnePixelSplitter().apply {
            firstComponent = left.scroll()
            secondComponent = right.scroll()
            this.splitterProportionKey = "sp-window-key"
            border = emptyBorder
        })
    }

    private fun getAllKeys() {
        DioApiService.getInstance().sendByAnyObject(mutableMapOf("action" to "SP_KEY"))
    }


    private fun createToolbar(): ActionToolbar {
        return ActionManager.getInstance().createActionToolbar("sp tool bar", DefaultActionGroup().apply {
            add(ActionManager.getInstance().getAction("FlutterProjects"))
            this.add(ActionManager.getInstance().getAction("shop.itbug.fluttercheckversionx.window.sp.SpRefreshAction"))
            this.add(HelpContextAction(SiteDocument.Sp))
        }, true).apply {
            this.targetComponent = toolWindow.component
        }
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        if (jsonObject != null) {
            val type = jsonObject["type"]
            if (type == SpManager.KEYS || type == SpManager.VALUE_GET) {
                SpManager(nativeMessage).handle()
            }
        }

    }

}


///刷新sp keys操作
class SpRefreshAction : DumbAwareAction(AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
        DioApiService.getInstance().sendByAnyObject(mutableMapOf("action" to "SP_KEY"))
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
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
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
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
    }


    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            DioApiService.getInstance()
                .sendByAnyObject(mutableMapOf("action" to "SP_GET_VALUE", "data" to selectedValue))
        }
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(200, -1)
    }

    override fun getMinimumSize(): Dimension {
        return preferredSize
    }

}

