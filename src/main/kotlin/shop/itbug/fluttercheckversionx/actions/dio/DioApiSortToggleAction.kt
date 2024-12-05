package shop.itbug.fluttercheckversionx.actions.dio

import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyToggleAction
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig


///接口排序方式
class DioApiSortToggleAction : MyToggleAction({ "Reverse List" }) {

    override fun isSelected(e: AnActionEvent): Boolean {
        return DioListingUiConfig.setting.isReverseApi
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        DioListingUiConfig.changeSetting { old -> old.copy(isReverseApi = state) }
    }
}