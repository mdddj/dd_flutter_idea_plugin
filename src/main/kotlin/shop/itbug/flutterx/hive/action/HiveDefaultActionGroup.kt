package shop.itbug.flutterx.hive.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import shop.itbug.flutterx.hive.model.HiveActionGetBox
import shop.itbug.flutterx.socket.service.AppService
import shop.itbug.flutterx.socket.service.DioApiService.Companion.getInstance


///获取盒子列表
class HiveGetBoxListAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = service<AppService>().currentSelectName.get() ?: ""
        getInstance().sendByAnyObject(HiveActionGetBox(projectName = project))
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = (service<AppService>().currentSelectName.get() ?: "").isNotEmpty()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}