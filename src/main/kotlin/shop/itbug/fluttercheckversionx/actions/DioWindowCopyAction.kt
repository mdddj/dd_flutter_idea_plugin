package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.toast

/**
 * 复制链接
 */
class DioWindowCopyAction : AnAction(PluginBundle.getLazyMessage("window.idea.dio.view.copy")) {
    override fun actionPerformed(e: AnActionEvent) {
        e.api()?.url?.copyTextToClipboard()?.apply {
            e.apiListProject()?.toast("Copy succeeded!")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.apiListProject() != null && e.api() != null
        super.update(e)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}