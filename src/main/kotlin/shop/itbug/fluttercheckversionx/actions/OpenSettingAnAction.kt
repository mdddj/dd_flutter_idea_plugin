package shop.itbug.fluttercheckversionx.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.setting.AppConfig

/**
 * 打开设置
 */
class OpenSettingAnAction : MyAction(AllIcons.General.Settings) {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.project, AppConfig::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    companion object {
        fun getInstance(): AnAction =
            ActionManager.getInstance().getAction("shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction")
    }
}