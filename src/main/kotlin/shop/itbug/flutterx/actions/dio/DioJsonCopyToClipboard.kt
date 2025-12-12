package shop.itbug.flutterx.actions.dio

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.actions.api
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.model.getMap
import shop.itbug.flutterx.socket.service.DioApiService
import shop.itbug.flutterx.util.CopyImageToClipboard

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
        e.presentation.icon = AllIcons.General.Copy
        e.presentation.isEnabled = e.api() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}