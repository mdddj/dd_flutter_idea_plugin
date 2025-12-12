package shop.itbug.flutterx.actions.riverpod

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareToggleAction
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.constance.MyKeys


/// riverpod setting
class RiverpodActionsSetting : DumbAwareToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        return PluginConfig.getState(e.project!!).showRiverpodInlay
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project
        if (project != null) {
            PluginConfig.changeState(project) { it.showRiverpodInlay = state }
            val element = e.getData(CommonDataKeys.EDITOR)?.getUserData(MyKeys.DartClassKey)
            if (element != null) {
                DaemonCodeAnalyzer.getInstance(project).restart(element.containingFile)
            }
        }

    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.project != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}
