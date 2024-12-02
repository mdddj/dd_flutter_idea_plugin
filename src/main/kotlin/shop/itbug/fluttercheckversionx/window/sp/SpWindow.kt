package shop.itbug.fluttercheckversionx.window.sp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
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
    DioApiService.NativeMessageProcessing, Disposable {


    private val left = SpWindowLeft()
    private val right = SpWindowRight(project)
    private val emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0)


    init {
        register()
        getAllKeys()
        addToTop(createToolbar().component)
        addToCenter(OnePixelSplitter().apply {
            firstComponent = left.scroll()
            secondComponent = right
            this.splitterProportionKey = "sp-window-key"
            border = emptyBorder
        })
        Disposer.register(this, right)
        Disposer.register(this, left)
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

    override fun dispose() {
        println("sp window disposed")
        removeMessageProcess()
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
private class SpWindowRight(project: Project) : JsonValueRender(project), Disposable {
    override fun dispose() {
        println("dispose sp window right disposed")
    }

    init {
        SpManagerListen.listen(this, null) {
            changeValue(it?.value)
        }
    }
}


/// keys 列表
private class SpWindowLeft : JBList<String>(), ListSelectionListener, Disposable {
    init {
        SpManagerListen.listen(this, modelHandel = {
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

    override fun dispose() {
        println("sp window left disposed")
    }

}

