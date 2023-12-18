package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.DumbAware
import shop.itbug.fluttercheckversionx.common.MyToggleAction
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

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
        val panel = e.getData<ApiListPanel>(DataKey.create(ApiListPanel.PANEL))
        panel?.let {
            it.autoscrolls = state
        }
        DioListingUiConfig.changeSetting { it.copy(autoScroller = state) }
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}