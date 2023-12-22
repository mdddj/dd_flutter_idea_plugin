package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareToggleAction
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.constance.MyKeys


/// riverpod setting
class RiverpodActionsSetting : DumbAwareToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        return PluginConfig.getState().showRiverpodInlay
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        PluginConfig.changeState { it.showRiverpodInlay = state }
        e.getData(CommonDataKeys.EDITOR)?.getUserData(MyKeys.DartClassKey)
            ?.let { dartElement: DartClassDefinitionImpl ->
                println("restart inlay provider...")
                DaemonCodeAnalyzer.getInstance(dartElement.project).restart(dartElement.containingFile)
            }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}
