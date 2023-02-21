package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

/**
 * 查看get参数
 */
open class DioWindowViewGetParamsAction : DumbAwareAction(PluginBundle.getLazyMessage("dio.toolbar.get.params")) {

    override fun actionPerformed(e: AnActionEvent) {
        val api = e.api()!!
         api.queryParams?.let { SimpleJsonViewDialog.show(it, e.apiListProject()!!) }

    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = e.api()?.queryParams?.isNotEmpty() == true && e.apiListProject() !=null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


    companion object {
        val instance: AnAction = ActionManager.getInstance().getAction("dio-window-view-GET")
    }

}