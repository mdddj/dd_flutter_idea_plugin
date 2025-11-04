package shop.itbug.fluttercheckversionx.actions.dio

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.TaskRunUtil

class DioStatusCheckAction: AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        p0.project?.let { project ->
            TaskRunUtil.runBackground(project,"FlutterX socket starting"){
                DioApiService.getInstance().tryStart(project)
                DioListingUiConfig.changeSetting { it.copy(enableFlutterXDioSocket = true) }
            }
        }

    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.project != null && !AppService.getInstance().dioIsStart
        e.presentation.text = "FlutterX socket not started yet."
        e.presentation.icon = AllIcons.General.Warning
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}