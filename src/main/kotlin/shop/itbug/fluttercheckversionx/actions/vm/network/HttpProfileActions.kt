package shop.itbug.fluttercheckversionx.actions.vm.network

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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