package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.OnePixelSplitter
import shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.config.DioxListingUiConfig
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.form.components.createDecorator
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import javax.swing.SwingUtilities

typealias Request = SocketResponseModel

// 监听http请求的窗口
class SocketRequestForm(val project: Project, private val toolWindow: ToolWindow) : OnePixelSplitter() {


    //项目筛选
    private val projectFilterBox = ProjectFilter()

    ///接口列表
    private val apiPanel = ApiListPanel(project)

    //一些 dio 的选项
    private val viewOptions =
        ActionManager.getInstance().getAction("Dio.Request.Item.Render.Option") as DefaultActionGroup


    //创建工具栏
//    private val toolbar = ActionManager.getInstance().createActionToolbar("Dio Toolbar",)

    //接口列表组件
    private var apiList = ListSpeedSearch(apiPanel) {
        it.url
    }.component
        .createDecorator {
            it
                .addExtraAction(projectFilterBox)
                .addExtraAction(DelButton().action)
                .addExtraAction(OpenSettingAnAction.getInstance())
                .addExtraAction(viewOptions)
        }


    //右侧面板
    private val rightFirstPanel = RightDetailPanel(project)


    init {
        firstComponent = apiList
        secondComponent = rightFirstPanel
        splitterProportionKey = SPLIT_KEY
        SocketMessageBus.listening {
            autoScrollToMax()
        }
    }


    /**
     * 自动滚动到最底部
     * todo : 滚动底部
     */
    private fun autoScrollToMax() {
        val setting = DioxListingUiConfig.setting
        if (setting.autoScroller) {
            SwingUtilities.invokeLater {
                println("滚动....到最底部")
                apiList.autoscrolls = setting.autoScroller
            }
        }
    }


    companion object {
        const val SPLIT_KEY = "Dio Panel Re Key"
    }


}



