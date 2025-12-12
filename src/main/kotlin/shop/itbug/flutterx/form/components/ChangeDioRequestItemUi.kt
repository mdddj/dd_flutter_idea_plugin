package shop.itbug.flutterx.form.components

import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.config.DioRequestUIStyle
import shop.itbug.flutterx.widget.MyComboActionNew

///切换dio请求ui样式
class ChangeDioRequestItemUi :
    MyComboActionNew.ToggleActionGroup<DioRequestUIStyle>(DioRequestUIStyle.entries.toTypedArray()) {
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