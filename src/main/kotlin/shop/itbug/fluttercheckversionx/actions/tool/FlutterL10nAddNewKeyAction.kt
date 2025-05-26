package shop.itbug.fluttercheckversionx.actions.tool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.FlutterL10nService
import shop.itbug.fluttercheckversionx.widget.WidgetUtil


// l10n 添加新的key
class FlutterL10nAddNewKeyAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project ?: return
        WidgetUtil.configTextFieldModal(
            project, PluginBundle.get("l10n.addDialog.labelText"), "It inserts this key into all ARB files"
        ) {
            FlutterL10nService.getInstance(project).insetNewKey(it)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.General.Add
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}