package shop.itbug.fluttercheckversionx.widget

import com.intellij.CommonBundle
import com.intellij.ide.GeneralSettings
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.PluginStateService

object PopupWidgets {

    fun showPluginLanguageSettings(e: AnActionEvent) {
        val languages = listOf("System", "English", "中文", "繁體", "한국어", "日本語")
        val group = DefaultActionGroup()
        lateinit var pop: ListPopup
        languages.forEach { lang ->
            group.add(object : ToggleAction(lang) {
                override fun isSelected(e: AnActionEvent): Boolean {
                    return PluginStateService.appSetting.lang == lang
                }

                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    if (state) {
                        PluginStateService.changeState {
                            it.copy(lang = lang)
                        }
                        pop.cancel()
                        restartWithConfirmation()
                    }
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }
            })
        }

        pop = JBPopupFactory.getInstance().createActionGroupPopup(
            PluginBundle.get("setting.language"),
            group,
            e.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
        e.project?.let { pop.showCenteredInCurrentWindow(it) }
    }

    fun restartWithConfirmation() {
        val app = ApplicationManagerEx.getApplicationEx()

        if (GeneralSettings.getInstance().isConfirmExit) {
            val answer = Messages.showYesNoDialog(
                IdeBundle.message(if (app.isRestartCapable) "dialog.message.restart.ide" else "dialog.message.restart.alt"),
                IdeBundle.message("dialog.title.restart.ide"),
                IdeBundle.message(if (app.isRestartCapable) "ide.restart.action" else "ide.shutdown.action"),
                CommonBundle.getCancelButtonText(),
                Messages.getQuestionIcon()
            )
            if (answer != Messages.YES) {
                return
            }
        }
        app.restart(true)
    }
}
