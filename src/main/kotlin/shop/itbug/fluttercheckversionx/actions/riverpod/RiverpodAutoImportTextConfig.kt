package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.widget.WidgetUtil

/**
 * 配置自动导入
 */
class RiverpodAutoImportTextConfig : AnAction() {
    private val title = PluginBundle.get("key_config") + PluginBundle.get("setting_riverpod_import_text_title")
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            val state = PluginConfig.getState(project)
            WidgetUtil.getTextEditorPopup(
                title,
                state.autoImportRiverpodText ?: "",
                state.autoImportRiverpodText,
                null, {
                    it.showCenteredInCurrentWindow(project)
                }) {
                PluginConfig.changeState(project) { s ->
                    s.autoImportRiverpodText = it
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            isVisible = e.project != null
            text = title
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
