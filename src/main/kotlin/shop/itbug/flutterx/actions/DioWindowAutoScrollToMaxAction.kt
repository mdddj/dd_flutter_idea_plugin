package shop.itbug.flutterx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import shop.itbug.flutterx.common.MyToggleAction
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.form.components.ApiListPanel
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.model.isDioRequest

/**
 * 自动滚动到最底部
 */
class DioWindowAutoScrollToMaxAction : MyToggleAction(PluginBundle.getLazyMessage("auto.scroll.to.the.bottom")),
    DumbAware {

    override fun isSelected(e: AnActionEvent): Boolean {
        val setting = DioListingUiConfig.setting
        return setting.autoScroller
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val panel = e.getData(ApiListPanel.PANEL_DATA_KEY)
        panel?.let {
            it.autoscrolls = state
        }
        DioListingUiConfig.changeSetting { it.copy(autoScroller = state) }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.api()?.isDioRequest() == true
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}