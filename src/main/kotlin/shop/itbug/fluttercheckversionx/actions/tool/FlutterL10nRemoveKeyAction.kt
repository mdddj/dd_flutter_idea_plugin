package shop.itbug.fluttercheckversionx.actions.tool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.MessageDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.FlutterL10nService
import shop.itbug.fluttercheckversionx.services.readValue
import shop.itbug.fluttercheckversionx.services.removeKey
import shop.itbug.fluttercheckversionx.window.l10n.MyL10nKeysTree

/**
 * 删除 l10n key 操作
 *
 * */
class FlutterL10nRemoveKeyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as MyL10nKeysTree
        val selectKey = tree.selectValue() ?: return

        val service = FlutterL10nService.getInstance(project)
        val arbFiles = service.arbFiles
        val text = arbFiles.joinToString("\n\n") { "${it.file.name}:\n ${it.readValue(selectKey)}" }
        val dialog =
            MessageDialogBuilder.yesNo(
                "${PluginBundle.get("delete_base_text")} Key",
                "${PluginBundle.get("ask_delete_all_l10n_key")} ($selectKey)" + "\n" + text
            )
        val isOk = dialog.ask(project)
        if (isOk) {
            val scope = service.coroutineScope()
            scope.launch {
                arbFiles.map {
                    scope.async {
                        it.removeKey(key = selectKey)
                    }
                }.awaitAll()
                service.refreshKeys()
                service.runFlutterGenL10nCommand()
            }

        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.General.Delete
        e.presentation.text = PluginBundle.get("delete_base_text") + " Key"
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? MyL10nKeysTree
        e.presentation.isEnabled = tree != null && tree.selectValue() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}