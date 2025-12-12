package shop.itbug.flutterx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.common.jsonToFreezedRun
import shop.itbug.flutterx.form.socket.getDataString
import shop.itbug.flutterx.form.socket.isParseToJson


///json 转 freezed
class DioRequestBodyToFreezedAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val currentSelectRequest = e.api()
        e.project?.jsonToFreezedRun(currentSelectRequest!!.getDataString())
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        val currentSelectRequest = e.api()
        e.presentation.isEnabled = currentSelectRequest?.isParseToJson() == true
        super.update(e)
    }
}
