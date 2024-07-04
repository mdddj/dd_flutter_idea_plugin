package shop.itbug.fluttercheckversionx.form.components

import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DioRequestUIStyle
import shop.itbug.fluttercheckversionx.widget.MyComboActionNew

///切换dio请求ui样式
class ChangeDioRequestItemUi :
    MyComboActionNew.ToggleActionGroup<DioRequestUIStyle>(DioRequestUIStyle.values()) {
    var setting = DioListingUiConfig.setting

    override var value: DioRequestUIStyle
        get() = setting.uiStyle
        set(value) {
            DioListingUiConfig.changeSetting { it.copy(uiStyle = value) }
            setting = DioListingUiConfig.setting
        }

    override fun getText(value: DioRequestUIStyle): String {
        return value.string
    }

}