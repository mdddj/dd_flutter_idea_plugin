package shop.itbug.flutterx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.widget.WidgetUtil

/**
 * 配置自动导入
 */
class RiverpodAutoImportTextConfig : AnAction() {
    private val title = PluginBundle.get("key_config") + " " + PluginBundle.get("setting_riverpod_import_text_title").lowercaseFirstLetter()
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

private fun String.lowercaseFirstLetter(): String {
    // 检查字符串是否为空，如果是则直接返回，避免异常
    if (this.isEmpty()) {
        return this
    }
    // 获取第一个字符并转换为小写，然后拼接上剩余的字符串
    return this[0].lowercase() + this.substring(1)
}