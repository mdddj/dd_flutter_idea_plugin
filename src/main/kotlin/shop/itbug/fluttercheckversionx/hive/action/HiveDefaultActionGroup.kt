package shop.itbug.fluttercheckversionx.hive.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.hive.model.HiveActionGetBox
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService


///获取盒子列表
class HiveGetBoxListAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = service<AppService>().currentSelectName.get() ?: ""
        DioApiService.sendByAnyObject(HiveActionGetBox(projectName = project))
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = (service<AppService>().currentSelectName.get() ?: "").isNotEmpty()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}