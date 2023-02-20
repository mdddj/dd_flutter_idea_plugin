package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.form.actions.DioRequestSearch
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.CardLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

typealias Request = SocketResponseModel

// 监听http请求的窗口
class SocketRequestForm(val project: Project, private val toolWindow: ToolWindow) : BorderLayoutPanel() {


    private val appService = service<AppService>()

    //项目筛选
    private val projectFilterBox = ProjectFilter()

    //搜索输入框
    private var searchTextField = DioRequestSearch {
    }
    //状态筛选,暂时弃用
//    private val stateCodeFilterBox = MethodFilter()

    //接口列表组件
    private var apiList = ApiListPanel(project)

    private val apiListWrapper = JBScrollPane(apiList)

    //右侧面板
    private val rightFirstPanel = RightDetailPanel(project)
    private val rightNextPanel = RequestDetailPanel(project)
    private val myRightComponent = JPanel(CardLayout()).apply {
        add(rightFirstPanel, "response_body_panel")
        add(rightNextPanel, "right_detail_panel")
    }

    //左侧工具栏
    private var leftToolBarCore: LeftActionTools =
        LeftActionTools(project, apiList, myRightComponent, rightNextPanel) {}


    //顶部组件
    private val createTopToolbarGroup: DefaultActionGroup = object : DefaultActionGroup() {
        init {
            this.add(projectFilterBox)
        }
    }

    //左侧大面板 - 顶部工具栏
    private val apiTopToolbar =
        createTopToolbarGroup.createWithToolbar(TOP_KET)
            .apply { targetComponent = toolWindow.component }.component.apply {
                this.add(searchTextField)
            }


    //左侧大面板 - 左边工具栏
    private val apiToolLeftToolbar =
        leftToolBarCore.createWithToolbar(LEFT_KEY, false).apply { targetComponent = toolWindow.component }.component


    //左侧大面板
    private val myFirstComponent = BorderLayoutPanel().apply {
        addToLeft(apiToolLeftToolbar)
        addToCenter(apiListWrapper)
        addToTop(BorderLayoutPanel().apply {
            addToRight(apiTopToolbar)
            addToCenter(searchTextField)
        })
    }

    //主面板
    private var mainPanel = OnePixelSplitter().apply {
        firstComponent = myFirstComponent
        secondComponent = myRightComponent
        splitterProportionKey = SPLIT_KEY
    }

    init {
        SwingUtilities.invokeLater {
            addToCenter(mainPanel)
        }
        SocketMessageBus.listening {
            autoScrollToMax()
        }
    }


    /**
     * 自动滚动到最底部
     */
    private fun autoScrollToMax() {
        if (appService.apiListAutoScrollerToMax) {
            SwingUtilities.invokeLater {
                apiListWrapper.verticalScrollBar.apply {
                    value = maximum
                }
            }
        }
    }


    companion object {
        const val SPLIT_KEY = "Dio Panel Re Key"
        const val TOP_KET = "Dio Top Toolbar Key"
        const val LEFT_KEY = "Dio Left Toolbar Key"
    }

}
