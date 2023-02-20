package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.DumbAwareAction
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.socket.Request

///查看get参数
open class DioWindowViewGetParamsAction : DumbAwareAction() {

    fun AnActionEvent.api(): Request? {
        return getData(DataKey.create(ApiListPanel.SELECT_KEY))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val api = e.api()!!
        val queryParams = api.queryParams!!
        SimpleJsonViewDialog.show(queryParams, e.project!!)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = e.api()?.queryParams?.isNotEmpty() == true && e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    companion object {
        fun ACTION(): DioWindowViewGetParamsAction = ActionManager.getInstance().getAction("dio-window-view-GET") as DioWindowViewGetParamsAction
    }
}