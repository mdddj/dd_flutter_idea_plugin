package shop.itbug.fluttercheckversionx.actions.bar

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.widget.PopupWidgets

class LanguagePopupSettingsAction : AnAction(), DumbAware {
    override fun actionPerformed(p0: AnActionEvent) {
        PopupWidgets.showPluginLanguageSettings(p0)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = PluginBundle
            .get("setting.language")
        e.presentation.icon = MyIcons.language
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}