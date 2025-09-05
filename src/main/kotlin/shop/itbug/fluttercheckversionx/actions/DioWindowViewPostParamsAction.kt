package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

/**
 * 查看post参数
 */
class DioWindowViewPostParamsAction : MyAction(PluginBundle.getLazyMessage("dio.toolbar.post.params")) {
    override fun actionPerformed(e: AnActionEvent) {
        val api = e.api()!!
        api.httpRequestBody?.let { SimpleJsonViewDialog.show(it, e.project!!) }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = e.api()?.httpRequestBody is Map<*, *> && e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}