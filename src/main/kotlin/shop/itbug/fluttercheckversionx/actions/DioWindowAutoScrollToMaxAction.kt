package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import shop.itbug.fluttercheckversionx.common.MyToggleAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService

/**
 * 自动滚动到最底部
 */
class DioWindowAutoScrollToMaxAction : MyToggleAction(PluginBundle.getLazyMessage("auto.scroll.to.the.bottom")),
    DumbAware {

    private val appService = service<AppService>()
    override fun isSelected(e: AnActionEvent): Boolean {
        return appService.apiListAutoScrollerToMax
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        appService.setIsAutoScrollToMax(state)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return  ActionUpdateThread.BGT
    }


}