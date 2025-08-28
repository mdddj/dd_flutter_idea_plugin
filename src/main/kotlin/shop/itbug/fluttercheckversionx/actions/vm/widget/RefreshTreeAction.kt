package shop.itbug.fluttercheckversionx.actions.vm.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.widget.FlutterWidgetTreeWidget

 val AnActionEvent.flutterTree get() = this.getData(FlutterWidgetTreeWidget.TREE_WIDGET)!!

//刷新树
class RefreshTreeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.flutterTree.refreshTree(false)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "RefreshTreeAction"
        e.presentation.icon = AllIcons.Actions.Refresh
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}