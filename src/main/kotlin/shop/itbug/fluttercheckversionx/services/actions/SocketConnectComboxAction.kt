package shop.itbug.fluttercheckversionx.services.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.widget.MyExpandableComboAction

/**
 * socket 链接列表
 */
class SocketConnectComboxAction : MyExpandableComboAction() {

    var selectSessionId = ""

    private fun createGroup() = object : DefaultActionGroup() {
        init {
            addAll(DioApiService.getInstance().getSessions().map {
                object : MyAction({ it.sessionID }) {
                    override fun actionPerformed(e: AnActionEvent) {
                        println("勾选。。。")
                        selectSessionId = it.sessionID
                    }
                }
            })
        }
    }


    override fun update(e: AnActionEvent) {
        val sessions = DioApiService.getInstance().getSessions()
        if (sessions.isEmpty()) {
            e.presentation.description = "Empty"
            e.presentation.text = PluginBundle.get("empty")
        }

        super.update(e)
    }


    override fun createPopup(event: AnActionEvent): JBPopup {
        val pop = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            createGroup(),
            event.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
        return pop
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
