package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.form.socket.getDataString
import shop.itbug.fluttercheckversionx.form.socket.isParseToJson
import shop.itbug.fluttercheckversionx.socket.service.AppService


///json 转 freezed
class DioRequestBodyToFreezedAction : AnAction() {


    override fun actionPerformed(e: AnActionEvent) {
        val currentSelectRequest = service<AppService>().currentSelectRequest
        e.project?.jsonToFreezedRun(currentSelectRequest!!.getDataString())
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        val currentSelectRequest = service<AppService>().currentSelectRequest
        e.presentation.isEnabled = currentSelectRequest?.isParseToJson() == true
        super.update(e)
    }
}
