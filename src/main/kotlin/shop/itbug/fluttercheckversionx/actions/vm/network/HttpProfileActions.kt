package shop.itbug.fluttercheckversionx.actions.vm.network

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.actions.api
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.toCurlStringAsDartDevTools
import shop.itbug.fluttercheckversionx.util.ComposeHelper

class HttpProfileCopyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val comp = e.getData(ComposeHelper.networkRequestDataKey)
        println(comp)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "拷贝"
        e.presentation.icon = AllIcons.Actions.Copy
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}


//拷贝 cURL
class HttpCopyToCurlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.api()!!.toCurlStringAsDartDevTools().copyTextToClipboard()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "${PluginBundle.get("copy")} cURL"
        e.presentation.isEnabled = e.api() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}