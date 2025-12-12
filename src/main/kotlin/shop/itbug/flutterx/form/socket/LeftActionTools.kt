package shop.itbug.flutterx.form.socket

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import shop.itbug.flutterx.socket.service.AppService


//清理接口列表
class DioRequestCleanAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        service<AppService>().cleanAllRequest()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}


fun DefaultActionGroup.create(place: String): ActionPopupMenu {
    return ActionManager.getInstance().createActionPopupMenu(place, this)
}

