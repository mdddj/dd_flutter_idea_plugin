package shop.itbug.fluttercheckversionx.window.logger.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.window.logger.MyLogPanel

// log 窗口 清理全部操作
class LoggerCleanAllAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        println(p0.getData(PlatformDataKeys.CONTEXT_COMPONENT))
        val myLogPanel = p0.logPanel() ?: return
        myLogPanel.removeAllItems()
    }

    override fun update(e: AnActionEvent) {
        val myLogPanel = e.logPanel()
        e.presentation.isEnabled = e.project != null && myLogPanel != null && myLogPanel.getListModel().isEmpty.not()
        e.presentation.icon = AllIcons.General.Delete
        e.presentation.text = PluginBundle.get("remove_all_data")

        super.update(e)
    }

    private fun AnActionEvent.logPanel(): MyLogPanel? {
        return this.getData(PlatformDataKeys.CONTEXT_COMPONENT) as MyLogPanel?
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}