package shop.itbug.fluttercheckversionx.actions.dio

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.actions.api
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.CopyImageToClipboard

/**
 * 拷贝接口数据为图片,并导入到剪贴板
 */
class DioJsonCopyToClipboard : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val apiInfo = e.api() ?: return
        val config = DioListingUiConfig.setting.copyKeys
        val text = DioApiService.getInstance().gson.toJson(apiInfo.getMap(config))
        CopyImageToClipboard.saveJsonStringAsImageToClipboard(e.project!!, text)
    }


    override fun update(e: AnActionEvent) {
        e.presentation.text = PluginBundle.get("copy_api_to_clipboard")
        e.presentation.icon = AllIcons.General.InlineCopy
        e.presentation.isEnabled = e.api() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}