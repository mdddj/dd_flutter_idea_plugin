package shop.itbug.fluttercheckversionx.actions.yaml

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import shop.itbug.fluttercheckversionx.util.RunUtil


//在 vscode中打开项目
class OpenInVsCodeAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.getData(CommonDataKeys.PROJECT)!!
        val file = p0.getData(CommonDataKeys.VIRTUAL_FILE)!!
        RunUtil.commandInBackground(
            project,
            "Opening vscode",
            { null },
            { it.message }
        ) {
            val command = GeneralCommandLine("code", ".")
            command.setWorkDirectory(file.path)
            command
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.VIRTUAL_FILE) != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}