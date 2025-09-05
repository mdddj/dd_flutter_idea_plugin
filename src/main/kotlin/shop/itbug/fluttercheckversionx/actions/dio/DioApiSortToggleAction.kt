package shop.itbug.fluttercheckversionx.actions.dio

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.actions.api
import shop.itbug.fluttercheckversionx.common.MyToggleAction
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.model.isDioRequest


///接口排序方式
class DioApiSortToggleAction : MyToggleAction({ "Reverse List" }) {

    override fun isSelected(e: AnActionEvent): Boolean {
        return DioListingUiConfig.setting.isReverseApi
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        DioListingUiConfig.changeSetting { old -> old.copy(isReverseApi = state) }
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.api()?.isDioRequest() == true
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}