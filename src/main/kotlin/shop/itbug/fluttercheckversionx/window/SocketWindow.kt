package shop.itbug.fluttercheckversionx.window

import com.intellij.execution.ui.RunContentManagerImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import shop.itbug.fluttercheckversionx.form.socket.SocketRequestForm
import shop.itbug.fluttercheckversionx.hive.HiveWidget
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.jobs.JobsWindow
import shop.itbug.fluttercheckversionx.window.sp.SpWindow

//是否开启找工作窗口
const val ENABLE_FIND_JOBS_WINDOW = false
const val ENABLE_CHAT_ROOM_WINDOW = false

/**
 * dio 的请求监听窗口, 在这个窗口中,会将手机里面的一系列请求在这个窗口中显示,并可以查看详细信息
 * 梁典典: 2022年04月29日11:12:51
 */
class SocketWindow : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val socketRequestForm = SocketRequestForm(project, toolWindow)
        val instance = ContentFactory.getInstance()
        val createContent = instance.createContent(socketRequestForm, PluginBundle.get("window.idea.dio.title"), false)
        toolWindow.contentManager.addContent(createContent)
        val port = PluginStateService.appSetting.serverPort.toInt() // dio的监听端口
        if (AppService.getInstance().dioIsStart.not()) {
            toolWindow.activate {
                try {
                    DioApiService.INSTANCESupplierSupplier.get().get().builder(port).start()
                    toolWindow.setIcon(RunContentManagerImpl.getLiveIndicator(MyIcons.flutter))
                    AppService.getInstance().setDioSocketState(true)
                } catch (e: Exception) {
                    project.toastWithError("Flutter dio listening service failed to start. Please try changing the port and restarting")
                }
            }
        } else {
            toolWindow.setIcon(RunContentManagerImpl.getLiveIndicator(MyIcons.flutter))
        }

        //在线聊天窗口
        if (ENABLE_CHAT_ROOM_WINDOW) {
            val flutterChatWindow = FlutterChatMessageWindow(project, toolWindow)
            val flutterChatWindowContent =
                instance.createContent(flutterChatWindow, PluginBundle.get("window.idea.chat.title"), false)
            toolWindow.contentManager.addContent(flutterChatWindowContent)
        }


        //找工作窗口
        if (ENABLE_FIND_JOBS_WINDOW) {
            val jobsWindow = JobsWindow(project, toolWindow)
            val jobsContent = instance.createContent(jobsWindow, "找工作", false)
            toolWindow.contentManager.addContent(jobsContent)
        }

        //api索引窗口
//        val apiIndexWindow = BaseApiIndexWindow(p0,p1)
//        val apiIndexContent = instance.createContent(apiIndexWindow,"接口管理",false)
//        p1.contentManager.addContent(apiIndexContent)


        // sp工具
        val spWindow = SpWindow(project, toolWindow)
        val spContent = instance.createContent(spWindow, "Shared Preferences ${PluginBundle.get("tool")}", false)
        toolWindow.contentManager.addContent(spContent)


        //hive 工具 开发中
        val hiveWindow = HiveWidget(project, toolWindow)
        val hiveContent = instance.createContent(hiveWindow, "Hive ${PluginBundle.get("tool")}", false)
        hiveContent.icon = AllIcons.General.Beta
        toolWindow.contentManager.addContent(hiveContent)
    }

}

//