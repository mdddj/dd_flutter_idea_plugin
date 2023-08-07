package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.toast

/**
 * 复制链接
 */
class DioWindowCopyAction : MyAction(PluginBundle.getLazyMessage("window.idea.dio.view.copy")) {
    override fun actionPerformed(e: AnActionEvent) {
        e.api()?.url?.copyTextToClipboard()?.apply {
            e.project?.toast("Copy succeeded!")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled =  e.api() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}