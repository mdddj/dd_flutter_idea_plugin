package shop.itbug.flutterx.actions.components

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.services.PubspecService
import shop.itbug.flutterx.util.RunUtil

class DartBuildAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        p0.project?.let {
            RunUtil.dartBuildInBackground(it)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible =
            project != null && PubspecService.getInstance(project).hasDependencies("build_runner")
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}