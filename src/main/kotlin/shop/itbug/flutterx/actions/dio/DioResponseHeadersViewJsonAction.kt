package shop.itbug.flutterx.actions.dio

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.actions.api
import shop.itbug.flutterx.dialog.SimpleJsonViewDialog
import shop.itbug.flutterx.i18n.PluginBundle

class DioResponseHeadersViewJsonAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.api()?.httpResponseHeaders?.apply { SimpleJsonViewDialog.show(this, e.project!!) }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.api()?.httpResponseHeaders?.isNotEmpty() == true
        e.presentation.text = "${PluginBundle.get("view.text")} Response Headers"
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}