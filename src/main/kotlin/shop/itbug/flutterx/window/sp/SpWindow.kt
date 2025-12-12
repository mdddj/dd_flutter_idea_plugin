package shop.itbug.flutterx.window.sp

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.flutterx.actions.context.HelpContextAction
import shop.itbug.flutterx.actions.context.SiteDocument
import shop.itbug.flutterx.common.scroll
import shop.itbug.flutterx.form.sub.JsonValueRender
import shop.itbug.flutterx.socket.service.DioApiService
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class SpWindow(project: Project, private val toolWindow: ToolWindow) : BorderLayoutPanel(),
    DioApiService.NativeMessageProcessing, Disposable {


    private val left = SpWindowLeft()
    private val right = SpWindowRight(project)


    init {
        register()
        getAllKeys()
        addToTop(createToolbar().component)
        addToCenter(OnePixelSplitter().apply {
            firstComponent = left.scroll()
            secondComponent = right
            this.splitterProportionKey = "sp-window-key"
            border = JBUI.Borders.customLineTop(JBColor.border())
        })
        putUserData(HelpContextAction.DataKey, SiteDocument.Sp)
        Disposer.register(this, right)
        Disposer.register(this, left)
    }

    private fun getAllKeys() {
        DioApiService.getInstance().sendByAnyObject(mutableMapOf("action" to "SP_KEY"))
    }


    private fun createToolbar(): ActionToolbar {
        val actionGroup = ActionManager.getInstance().getAction("SpPanelToolbar") as DefaultActionGroup
        return ActionManager.getInstance().createActionToolbar("sp tool bar", actionGroup, true).apply {
            this.targetComponent = toolWindow.component
        }
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        val jsonDataString = jsonObject?.get("jsonDataString") as? String
        if (jsonObject != null && jsonDataString != null) {
            val type = jsonObject["type"]
            if (type == SpManager.KEYS || type == SpManager.VALUE_GET) {
                SpManager(jsonDataString).handle()
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
class SpWindowLeft : JBList<String>(), ListSelectionListener, Disposable, UiDataProvider {
    init {
        SpManagerListen.listen(this, modelHandel = {
            this.model = DefaultListModel<String>().apply {
                this.addAll(it.keys)
            }
        }, valueHandle = null)
        addListSelectionListener(this)
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        PopupHandler.installPopupMenu(this, "SPRightMenuAction", "SP_WINDOW_LEFT_MENU")
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            DioApiService.getInstance()
                .sendByAnyObject(mutableMapOf("action" to "SP_GET_VALUE", "data" to selectedValue))
        }
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(200, super<JBList>.preferredSize.height)
    }

    override fun getMinimumSize(): Dimension {
        return preferredSize
    }

    override fun dispose() {
        println("sp window left disposed")
    }

    override fun uiDataSnapshot(sink: DataSink) {

    }

}

