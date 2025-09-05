package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

/**
 * 查看请求头操作
 */
class DioWindowViewHeadersAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.api()?.httpRequestHeaders?.apply { SimpleJsonViewDialog.show(this, e.project!!) }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.api()?.httpRequestHeaders?.isNotEmpty() == true
        e.presentation.text = PluginBundle.get("view.request.headers")
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}