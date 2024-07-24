package shop.itbug.fluttercheckversionx.form.socket

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.dialog.validParseToFreezed
import shop.itbug.fluttercheckversionx.form.actions.DioRequestSearch
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.DioApiService


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
            (ActionManager.getInstance().getAction("FlutterX Window Top Actions") as DefaultActionGroup)


        //创建工具栏
        private val topToolbar =
            ActionManager.getInstance().createActionToolbar("Dio Toolbar", topActions, true)


        private val apiList = apiPanel.scroll().apply {
            border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
        }

        init {
            topToolbar.targetComponent = toolWindow.component
            addToTop(BorderLayoutPanel().apply {
                addToCenter(DioRequestSearch())
                addToRight(topToolbar.component)
            })
            addToCenter(apiList)

        }


        ///滚动到底部.
        fun scrollToBottom() {
            val verticalScrollBar = apiList.verticalScrollBar
            verticalScrollBar.value = verticalScrollBar.maximum
        }
    }

    /**
     * 自动滚动到最底部
     */
    private fun autoScrollToMax() {
        val setting = DioListingUiConfig.setting
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



