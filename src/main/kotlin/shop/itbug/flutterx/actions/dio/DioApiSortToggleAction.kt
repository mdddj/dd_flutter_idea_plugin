package shop.itbug.flutterx.actions.dio

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.actions.api
import shop.itbug.flutterx.common.MyToggleAction
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.model.isDioRequest


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