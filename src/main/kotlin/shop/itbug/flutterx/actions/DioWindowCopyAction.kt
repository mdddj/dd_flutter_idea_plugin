package shop.itbug.flutterx.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.common.MyAction
import shop.itbug.flutterx.document.copyTextToClipboard
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.util.toast

/**
 * 复制链接
 */
class DioWindowCopyAction : MyAction(PluginBundle.getLazyMessage("window.idea.dio.view.copy")) {
    override fun actionPerformed(e: AnActionEvent) {
        e.api()?.requestUrl?.copyTextToClipboard()?.apply {
            e.project?.toast("Copy succeeded!")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.api() != null
        e.presentation.icon = AllIcons.Actions.Copy
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}