package shop.itbug.flutterx.actions.tool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.panel
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.dialog.MyRowBuild
import shop.itbug.flutterx.services.FlutterL10nService
import shop.itbug.flutterx.tools.flutterLibFolder
import java.awt.Dimension
import javax.swing.JComponent

/**
 * 更新 l10n目录的弹窗操作
 */
class FlutterL10nSettingChangeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project -> MySettingDialog(project).show() }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.project != null
        e.presentation.icon = AllIcons.General.Settings
        super.update(e)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    companion object {
        fun getInstance(): FlutterL10nSettingChangeAction {
            return ActionManager.getInstance()
                .getAction("FlutterL10nSettingChangeAction") as FlutterL10nSettingChangeAction
        }
    }
}

class MySettingDialog(val project: Project) : DialogWrapper(null, true) {
    val config = PluginConfig.getState(project)
    private val service = FlutterL10nService.getInstance(project)
    private lateinit var myPanel: DialogPanel

    init {
        super.init()
        title = "FlutterX l10n settings"
    }

    override fun createCenterPanel(): JComponent {

        myPanel = panel {
            row {
                MyRowBuild.folder(
                    this,
                    { config.l10nFolder ?: (project.flutterLibFolder()?.path ?: "") },
                    { config.l10nFolder = it },
                    project
                )
                    .label("l10n folder", LabelPosition.TOP)
            }
            row {
                MyRowBuild.changeDefaultL10nFile(this, {
                    config.l10nDefaultFileName ?: ""
                }, {
                    config.l10nDefaultFileName = it
                }, project).label("Default preview text file", LabelPosition.TOP)
                    .align(Align.FILL)
            }
        }
        myPanel.preferredSize = Dimension(400, -1)
        return myPanel
    }


    override fun doOKAction() {
        myPanel.apply()
        super.doOKAction()
        ApplicationManager.getApplication().invokeLater {
            service.configEndTheL10nFolder()
        }
    }
}