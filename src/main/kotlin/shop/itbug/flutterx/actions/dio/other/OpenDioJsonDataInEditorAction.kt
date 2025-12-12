package shop.itbug.flutterx.actions.dio.other

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.model.openJsonDataInEditor
import shop.itbug.flutterx.socket.service.DioApiService

class OpenDioJsonDataInEditorAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val api = DioApiService.getInstance().getCurrentSelectApi(p0)
        api?.openJsonDataInEditor(p0.project!!)
    }


    override fun update(p0: AnActionEvent) {
        val api = DioApiService.getInstance().getCurrentSelectApi(p0)
        p0.presentation.isEnabled = p0.project != null && api != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}