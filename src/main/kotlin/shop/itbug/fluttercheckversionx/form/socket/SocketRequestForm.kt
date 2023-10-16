package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.config.DioxListingUiConfig
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.DioApiService


typealias Request = SocketResponseModel

// 监听http请求的窗口
class SocketRequestForm(val project: Project, private val toolWindow: ToolWindow) : OnePixelSplitter(),
    DioApiService.HandleFlutterApiModel {


    ///接口列表
    private val apiPanel = ApiListPanel(project)


    private val leftPanel = LeftPanel()


    //右侧面板
    private val rightFirstPanel = RightDetailPanel(project)


    init {
        firstComponent = leftPanel
        secondComponent = rightFirstPanel
        splitterProportionKey = SPLIT_KEY
        register()
    }


    private inner class LeftPanel : BorderLayoutPanel() {
        private val topActions =
            ActionManager.getInstance().getAction("FlutterX Window Top Actions") as DefaultActionGroup

        private val leftActions =
            ActionManager.getInstance().getAction("FlutterX window Left Action") as DefaultActionGroup

        //创建工具栏
        private val topToolbar = ActionManager.getInstance().createActionToolbar("Dio Toolbar", topActions, true)

        private val leftToolbar = ActionManager.getInstance().createActionToolbar("Dio Left Action", leftActions, false)

        private val apiList = apiPanel.scroll().apply {
            border = null
        }

        init {
            topToolbar.targetComponent = toolWindow.component
            leftToolbar.targetComponent = toolWindow.component
            addToTop(topToolbar.component)
            addToCenter(apiList)
            addToLeft(leftToolbar.component)
        }


        ///滚动到底部.
        fun scrollToBottom() {
            println("滚动到底部.")
            val verticalScrollBar = apiList.verticalScrollBar
            verticalScrollBar.value = verticalScrollBar.maximum
        }
    }

    /**
     * 自动滚动到最底部
     * todo : 滚动底部
     */
    private fun autoScrollToMax() {
        val setting = DioxListingUiConfig.setting
        if (setting.autoScroller) {
            ApplicationManager.getApplication().invokeLater {
                leftPanel.scrollToBottom()
            }
        }
    }


    override fun handleModel(model: SocketResponseModel) {
        autoScrollToMax()
    }

    companion object {
        const val SPLIT_KEY = "Dio Panel Re Key"
    }


}



