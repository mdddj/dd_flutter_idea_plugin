package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.socket.Request
import shop.itbug.fluttercheckversionx.socket.service.DioApiService

// ApiCopyAll类继承自AnAction，用于处理复制所有API相关操作的逻辑
class ApiCopyAll : AnAction() {
    // 当动作被触发时执行的方法，主要逻辑是将API请求的相关信息转换为JSON字符串并复制到剪贴板
    override fun actionPerformed(e: AnActionEvent) {
        val api: Request = e.api()!!
        val config = DioListingUiConfig.setting.copyKeys
        val string = DioApiService.getInstance().gson.toJson(api.getMap(config))
        string.copyTextToClipboard()
    }

    // 更新动作状态的方法，设置动作的显示文本和是否启用
    override fun update(e: AnActionEvent) {
        e.presentation.text = "Copy All"
        e.presentation.isEnabled = e.api() != null
        super.update(e)
    }

    // 获取动作更新线程的方法，返回后台线程
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
