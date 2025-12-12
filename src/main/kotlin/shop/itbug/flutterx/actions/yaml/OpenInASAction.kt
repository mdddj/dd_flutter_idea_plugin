package shop.itbug.flutterx.actions.yaml

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.ProjectManager

//在 android studio 中打开项目
class OpenInASAction: AnAction() {

    override fun actionPerformed(p0: AnActionEvent) {
        val file = p0.getData(CommonDataKeys.VIRTUAL_FILE)!!
        ProjectManager.getInstance().loadAndOpenProject(file.path)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.VIRTUAL_FILE) != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}