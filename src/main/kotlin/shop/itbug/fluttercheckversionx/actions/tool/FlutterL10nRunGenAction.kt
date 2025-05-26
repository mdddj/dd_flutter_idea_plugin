package shop.itbug.fluttercheckversionx.actions.tool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.services.FlutterL10nService

class FlutterL10nRunGenAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            FlutterL10nService.getInstance(project).runFlutterGenL10nCommand()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.project != null
        e.presentation.icon = AllIcons.Debugger.ThreadRunning
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}