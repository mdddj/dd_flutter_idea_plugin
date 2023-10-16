package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService


//清理接口列表
class DioRequestCleanAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        service<AppService>().cleanAllRequest()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.Actions.GC
        e.presentation.description = PluginBundle.get("window.idea.dio.view.clean.desc")
        e.presentation.text = PluginBundle.get("clean")
        super.update(e)
    }

}


fun DefaultActionGroup.create(place: String): ActionPopupMenu {
    return ActionManager.getInstance().createActionPopupMenu(place, this)
}

