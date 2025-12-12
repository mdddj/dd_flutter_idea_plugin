package shop.itbug.flutterx.actions.tool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.services.FlutterL10nService

class FlutterL10nWindowTreeRefreshAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            FlutterL10nService.getInstance(project).refreshKeys()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.Actions.Refresh
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    companion object {
        fun getAction(): AnAction = ActionManager.getInstance().getAction("FlutterL10nWindowTreeRefreshAction")
    }
}
