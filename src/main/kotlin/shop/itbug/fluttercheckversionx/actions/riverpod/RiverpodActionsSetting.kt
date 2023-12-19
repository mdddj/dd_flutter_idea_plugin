package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.util.restartAnalyzer


///设置
class RiverpodActionsSetting : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        return PluginConfig.getState().showRiverpodInlay
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val get = PluginConfig.getInstance()
        val newState = get.state.copy(showRiverpodInlay = state)
        get.loadState(newState)
        e.project?.restartAnalyzer()

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}
