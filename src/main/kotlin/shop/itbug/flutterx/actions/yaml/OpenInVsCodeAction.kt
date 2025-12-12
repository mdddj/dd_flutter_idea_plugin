package shop.itbug.flutterx.actions.yaml

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import shop.itbug.flutterx.util.RunUtil


//在 vscode中打开项目
class OpenInVsCodeAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        RunUtil.runOpenInBackground(p0,"Opening vscode"){
            GeneralCommandLine("code", ".")
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