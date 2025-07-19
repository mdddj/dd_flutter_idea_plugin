package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.toast

object MyToolWindowTools {

    const val WINDOW_ID: String = "Dio Request"


    fun getMyToolWindow(project: Project): ToolWindow? {
        return ToolWindowManager.getInstance(project = project).getToolWindow(WINDOW_ID)
    }

    /**
     * 重启 dio socket 监听
     */
    fun resetDioRequestListenServer(project: Project) {
        getMyToolWindow(project)?.let {
            if (it.isDisposed) {
                return
            }
            it.activate(DioApiService.getInstance().createServerRunner(project, it))
            project.toast("${PluginBundle.get("reset_success")},port:${PluginStateService.appSetting.serverPort}")
        }
    }

    /**
     * 关机
     */
    fun setToolWindowNullActive(project: Project) {
        getMyToolWindow(project)?.let {
            if (!it.isDisposed) {
                it.activate(null)
                it.setIcon(MyIcons.flutter)
                AppService.getInstance().setDioSocketState(false)
            }
        }
    }
}