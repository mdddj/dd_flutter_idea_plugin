package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import shop.itbug.fluttercheckversionx.socket.service.AppService

///是否展示详情面板
class DioWindowShowDetailPanel : ToggleAction(), DumbAware {

    private val appService = service<AppService>()
    override fun isSelected(e: AnActionEvent): Boolean {
        return appService.apiListAutoScrollerToMax
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        appService.setIsAutoScrollToMax(state)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}