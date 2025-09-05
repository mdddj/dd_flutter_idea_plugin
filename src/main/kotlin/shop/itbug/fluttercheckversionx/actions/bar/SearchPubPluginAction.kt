package shop.itbug.fluttercheckversionx.actions.bar

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.options.ShowSettingsUtil
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.constance.Links
import shop.itbug.fluttercheckversionx.constance.discordUrl
import shop.itbug.fluttercheckversionx.constance.qqGroup
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.setting.AppConfig
import shop.itbug.fluttercheckversionx.tools.FlutterProjectUtil
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
        e.project?.let {
            e.presentation.isVisible = PluginConfig.getState(it).showDiscord
        }
        e.presentation.text = "Discord"
    }
}

// open setting
class GoToQQGroupAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(qqGroup)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.project?.let {
            e.presentation.isVisible = PluginConfig.getState(it).showQQGroup
        }
        e.presentation.text = "QQ Group"
    }
}

// go to Discord
// open setting
class GoToDocumentAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(Links.DOCUMENT_DEFAULT_URL)
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
        e.presentation.icon = AllIcons.General.Settings
    }
}


// 在Android studio 打开项目目录
class OpenAndroidProjectOnASAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            FlutterProjectUtil(it).openAndroidStudioWithDirectory()
        }
    }

    override fun update(e: AnActionEvent) {


        e.presentation.isEnabledAndVisible = e.project != null
        e.project?.let {
            val config = PluginConfig.getInstance(it).state
            e.presentation.isVisible = config.openAndroidDirectoryInAS
        }


        e.project?.let {
            val tool = FlutterProjectUtil(it)
            e.presentation.isVisible = tool.androidDirIsExist
            e.presentation.isEnabled = tool.isAndroidStudioInstalled
        }
        e.presentation.icon = MyIcons.androidStudio
        e.presentation.text = "Open Android Project"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}


/**
 * 在xcode打开ios目录
 */
class OpenIosProjectInXcodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            val tool = FlutterProjectUtil(it)
            tool.openIosInXCode()
        }
    }

    override fun update(e: AnActionEvent) {
        e.project?.let {
            val tool = FlutterProjectUtil(it)
            val config = PluginConfig.getInstance(it).state
            e.presentation.isVisible =
                e.project != null && tool.isMacos && tool.iosDirIsExist && config.openIosDirectoryInXcode
        }
        e.presentation.text = "Open Ios Project"
        e.presentation.icon = MyIcons.xcode
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}

/**
 * 在xcode打开macos目录
 */
class OpenMacosProjectInXcodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            val tool = FlutterProjectUtil(it)
            tool.openMacosInXCode()
        }
    }

    override fun update(e: AnActionEvent) {
        e.project?.let {
            val tool = FlutterProjectUtil(it)
            val config = PluginConfig.getInstance(it).state
            e.presentation.isVisible =
                e.project != null && tool.isMacos && tool.macosDirIsExist && config.openMacosDirectoryInXcode
        }
        e.presentation.text = "Open Macos Project"
        e.presentation.icon = MyIcons.xcode
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}



