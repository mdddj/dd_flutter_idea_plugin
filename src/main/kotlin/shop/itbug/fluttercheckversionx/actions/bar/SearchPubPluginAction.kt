package shop.itbug.fluttercheckversionx.actions.bar

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.options.ShowSettingsUtil
import shop.itbug.fluttercheckversionx.constance.discordUrl
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.setting.AppConfig
import shop.itbug.fluttercheckversionx.util.RunUtil


fun getStatusBarActionGroup() = ActionManager.getInstance().getAction("status_bar_actions") as DefaultActionGroup

/// dart package search
class SearchPubPluginAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { SearchDialog(it).show() }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = PluginBundle.get("search.pub.plugin")
    }
}

// run builder
class FlutterRunBuilderCommandAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { RunUtil.runCommand(it, "FlutterX run build", "flutter pub run build_runner build") }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = PluginBundle.get("run.build_runner.build")
    }
}

// json to freezed
class JsonToFreezedClass : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { JsonToFreezedInputDialog(it).show() }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = "Json to Freezed"
    }
}


// go to Discord
// open setting
class GoToDiscordAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(discordUrl)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = "Discord"
    }
}

// go to Discord
// open setting
class GoToDocumentAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse("https://flutterx.itbug.shop")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = PluginBundle.get("document")
    }
}

// open setting
class OpenSettingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.project, AppConfig::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.text = PluginBundle.get("open_flutterx_setting")
    }
}

