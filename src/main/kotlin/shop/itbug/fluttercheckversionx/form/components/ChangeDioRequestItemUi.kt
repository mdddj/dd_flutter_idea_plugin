package shop.itbug.fluttercheckversionx.form.components

import shop.itbug.fluttercheckversionx.config.DioRequestUIStyle
import shop.itbug.fluttercheckversionx.config.DioxListingUiConfig
import shop.itbug.fluttercheckversionx.widget.MyComboActionNew

///切换dio请求ui样式
class ChangeDioRequestItemUi() :
    MyComboActionNew.ToggleActionGroup<DioRequestUIStyle>(DioRequestUIStyle.entries.toTypedArray()) {
    var setting = DioxListingUiConfig.setting

    override var value: DioRequestUIStyle
        get() = setting.uiStyle
        set(value) {
            DioxListingUiConfig.changeSetting { it.copy(uiStyle = value) }
            setting = DioxListingUiConfig.setting
        }

    override fun getText(value: DioRequestUIStyle): String {
        return value.string
    }

}