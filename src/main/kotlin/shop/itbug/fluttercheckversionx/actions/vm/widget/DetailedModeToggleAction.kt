package shop.itbug.fluttercheckversionx.actions.vm.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction


/**
 * 详细模式切换Action
 * 用于切换是否显示Text内容和其他详细信息
 */
class DetailedModeToggleAction() : ToggleAction(
    "Toggle Text Preview",
    "Show/Hide Text content in widget tree (may cause deep nesting issues)",
    AllIcons.Actions.ShowAsTree
) {

    override fun isSelected(e: AnActionEvent): Boolean {
        return e.flutterTree?.detailedMode ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.flutterTree?.detailedMode = state
        e.flutterTree?.refreshTree(false)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.flutterTree!=null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}