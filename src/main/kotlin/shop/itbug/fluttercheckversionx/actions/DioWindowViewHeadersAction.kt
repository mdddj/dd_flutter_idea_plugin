package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

/**
 * 查看请求头操作
 */
class DioWindowViewHeadersAction : MyAction(PluginBundle.getLazyMessage("view.request.headers")) {
    override fun actionPerformed(e: AnActionEvent) {
        e.api()?.headers?.apply { SimpleJsonViewDialog.show(this, e.project!!) }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project!=null && e.api()?.headers?.isNotEmpty() == true
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}