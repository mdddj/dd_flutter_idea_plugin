package shop.itbug.fluttercheckversionx.actions.dio.hurl

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.actions.api
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.socket.HurlGenerate
import shop.itbug.fluttercheckversionx.socket.Request


abstract class HurlCopyBaseAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val api = e.api()
        if (api != null) {
            doCopy(api, api.hurlGenerate, e)?.copyTextToClipboard()
        }
    }

    abstract fun doCopy(request: Request, generate: HurlGenerate, e: AnActionEvent): String?

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.Actions.Copy
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}


///基础
class HurlCopyBaseActionImpl : HurlCopyBaseAction() {
    override fun doCopy(
        request: Request,
        generate: HurlGenerate,
        e: AnActionEvent
    ): String? {
        val base = generate.base()
        return base
    }
}

