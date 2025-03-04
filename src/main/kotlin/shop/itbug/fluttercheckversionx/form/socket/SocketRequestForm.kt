package shop.itbug.fluttercheckversionx.form.socket

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.dialog.validParseToFreezed
import shop.itbug.fluttercheckversionx.form.actions.DioRequestSearch
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener


typealias Request = SocketResponseModel

///是否可以转换到 json 对象
fun SocketResponseModel.isParseToJson(): Boolean {
    data.let {
        when (it) {
            is String -> {
                return it.validParseToFreezed()
            }

            is Map<*, *> -> {
                return true
            }

            is List<*> -> {
                return true
            }

            else -> false
        }
    }
    return false
}

fun SocketResponseModel.getDataString(): String {
    data.let {
        return when (it) {
            is String -> it
            is Map<*, *> -> Gson().toJson(it)
            is List<*> -> Gson().toJson(it)
            else -> "$it"
        }
    }
}

// 监听http请求的窗口
class SocketRequestForm(val project: Project, private val toolWindow: ToolWindow) : OnePixelSplitter(),
    DioApiService.HandleFlutterApiModel, Disposable, ListDataListener, ApiListPanel.OnChangeConfigListen {


    ///接口列表
    val apiPanel = ApiListPanel(project).also { it.addOnChangeConfigListen(this) }


    private val apiList = apiPanel.scroll().apply {
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
    }

    private val leftPanel = LeftPanel()


    //右侧面板
    private val rightFirstPanel = RightDetailPanel(project)


    init {
        firstComponent = leftPanel
        secondComponent = rightFirstPanel
        splitterProportionKey = SPLIT_KEY
        register()
        Disposer.register(this, apiPanel)
        apiPanel.model.addListDataListener(this)
    }


    ///滚动到底部.
    private fun scrollToBottom() {
        val verticalScrollBar = apiList.verticalScrollBar
        verticalScrollBar.value = verticalScrollBar.maximum
        apiPanel.ensureIndexIsVisible(apiPanel.model.size - 1)
    }

    ///滚动到底部.
    private fun scrollToTop() {
        val verticalScrollBar = apiList.verticalScrollBar
        verticalScrollBar.value = 0
        apiPanel.ensureIndexIsVisible(0)
    }


    private inner class LeftPanel : BorderLayoutPanel() {
        private val topActions =
            (ActionManager.getInstance().getAction("FlutterX Window Top Actions") as DefaultActionGroup)


        //创建工具栏
        private val topToolbar =
            ActionManager.getInstance().createActionToolbar("Dio Toolbar", topActions, true)


        init {
            topToolbar.targetComponent = toolWindow.component
            addToTop(BorderLayoutPanel().apply {
                addToCenter(DioRequestSearch())
                addToRight(topToolbar.component)
            })
            addToCenter(apiList)

        }


    }

    /**
     * 自动滚动到最底部
     */
    private fun autoScrollToMax() {
        val setting = DioListingUiConfig.setting
        if (setting.autoScroller) {
            ApplicationManager.getApplication().invokeLater {
                scrollToBottom()
            }
        }
    }


    override fun handleModel(model: SocketResponseModel) {
        if (project.isDisposed) return
        autoScrollToMax()
    }


    companion object {
        const val SPLIT_KEY = "Dio Panel Re Key"
    }


    override fun dispose() {
        println("dispose.....SocketRequestForm")
        apiPanel.model.removeListDataListener(this)
        apiPanel.removeOnChangeConfigListen(this)
        DioApiService.getInstance().dispose()
        super.dispose()
    }

    override fun intervalAdded(p0: ListDataEvent?) {
        autoScrollToMax()
    }

    override fun intervalRemoved(p0: ListDataEvent?) {
        autoScrollToMax()
    }

    override fun contentsChanged(p0: ListDataEvent?) {
        autoScrollToMax()
    }

    override fun listenChanged(
        p1: DoxListeningSetting,
        p2: DoxListeningSetting
    ) {
        if (p1.isReverseApi != p2.isReverseApi) {
            ApplicationManager.getApplication().invokeLater {
                if (p2.isReverseApi) {
                    //到顶部
                    scrollToTop()
                } else {
                    //到底部
                    scrollToBottom()
                }

            }
        }
    }

}



