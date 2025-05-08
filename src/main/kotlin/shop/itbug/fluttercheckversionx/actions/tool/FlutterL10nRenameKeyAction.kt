package shop.itbug.fluttercheckversionx.actions.tool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.FlutterL10nService
import shop.itbug.fluttercheckversionx.services.renameKey
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import shop.itbug.fluttercheckversionx.window.l10n.MyL10nKeysTree

/**
 * 重新命名 key
 */
class FlutterL10nRenameKeyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as MyL10nKeysTree
        val selectKey = tree.selectValue() ?: return
        val service = FlutterL10nService.getInstance(project)
        val scope = service.coroutineScope()
        val arbFiles = service.arbFiles
        WidgetUtil.configTextFieldModal(
            project = project,
            labelText = "${PluginBundle.get("rename")} Key",
            initValue = selectKey,
            handle = { newKey ->
                if (newKey.isBlank()) {
                    return@configTextFieldModal
                }
                scope.launch {
                    arbFiles.map { scope.async { it.renameKey(selectKey, newKey) } }.awaitAll()
                    service.refreshKeys()
                    service.runFlutterGenL10nCommand()
                }
            }
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.Actions.Edit
        e.presentation.text = PluginBundle.get("rename") + " Key"
        val tree = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? MyL10nKeysTree
        e.presentation.isEnabled = tree != null && tree.selectValue() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}