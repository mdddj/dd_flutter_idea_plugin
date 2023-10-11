package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.components.ChangeDioRequestItemUi
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.form.components.createDecorator
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.SwingUtilities

typealias Request = SocketResponseModel

// 监听http请求的窗口
class SocketRequestForm(val project: Project, private val toolWindow: ToolWindow) : BorderLayoutPanel(),
    DioApiService.HandleFlutterApiModel {


    private val appService = service<AppService>()

    //项目筛选
    private val projectFilterBox = ProjectFilter()

    //接口列表组件
    private var apiList = ListSpeedSearch(ApiListPanel(project)) {
        it.url
    }.apply {

    }.component
        .createDecorator {
        it
            .addExtraAction(projectFilterBox)
//            .addExtraAction(ActionManager.getInstance().getAction("shop.itbug.fluttercheckversionx.services.actions.SocketConnectComboxAction"))
            .addExtraAction(ChangeDioRequestItemUi())
            .addExtraAction(DelButton().action)
            .addExtraAction(OpenSettingAnAction.getInstance())

    }



    private val apiListWrapper = JBScrollPane(apiList).apply {
        border = null
    }



    //右侧面板
    private val rightFirstPanel = RightDetailPanel(project)


    //主面板
    private var mainPanel = OnePixelSplitter().apply {
        firstComponent = apiListWrapper
        secondComponent = rightFirstPanel
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
     * todo : 滚动底部
     */
    private fun autoScrollToMax() {
        if (appService.apiListAutoScrollerToMax) {
            SwingUtilities.invokeLater {
                apiListWrapper.verticalScrollBar.apply {
                    value = maximum + 20
                }
                ///滚动到地步
            }
        }
    }


    companion object {
        const val SPLIT_KEY = "Dio Panel Re Key"
        const val TOP_KET = "Dio Top Toolbar Key"
        const val LEFT_KEY = "Dio Left Toolbar Key"
    }

    override fun handleModel(model: SocketResponseModel) {
        val flutterProjects = appService.flutterProjects
        val reqs = flutterProjects[model.projectName] ?: emptyList()
        val reqsAdded = reqs.plus(model)
        model.projectName?.apply {
            if (!flutterProjects.keys.contains(this)) {
                val old = mutableListOf<String>()
                flutterProjects.keys.forEach {
                    old.add(it)
                }
                old.add(this)
                appService.fireFlutterNamesChangeBus(old.toList())
            }

            flutterProjects[this] = reqsAdded
        }
        appService.projectNames = flutterProjects.keys.toList()
        SocketMessageBus.fire(model)
    }

    override fun stateEvent(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {

    }

    override fun covertJsonError(e: Exception, aio: AioSession?) {
        e.printStackTrace()
        project.toastWithError("$e")
    }

}



