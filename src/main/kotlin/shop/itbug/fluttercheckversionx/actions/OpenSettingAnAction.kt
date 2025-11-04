package shop.itbug.fluttercheckversionx.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.setting.AppConfig

/**
 * 打开设置
 */
class OpenSettingAnAction : DumbAwareAction({ PluginBundle.get("setting.flutterx") }, AllIcons.General.Settings) {
    override fun actionPerformed(e: AnActionEvent) {
        thisLogger().debug("打开系统设置")
        e.project?.let { ShowSettingsUtil.getInstance().showSettingsDialog(it, AppConfig::class.java) }
    }
}