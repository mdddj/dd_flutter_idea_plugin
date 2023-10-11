package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service


///ui 渲染样式
enum class DioRequestUIStyle(val string: String) {
    DefaultStyle("Default"), CompactStyle("Compact")
}

data class DioxListeningSetting(
    //是否展示前缀host
    var showHost: Boolean = true,
    //是否展示get后缀查询参数
    var showQueryParams: Boolean = true,
    //显示请求方法
    var showMethod: Boolean = true,
    //显示接口状态码
    var showStatusCode: Boolean = true,
    //显示接口耗时
    var showTimestamp: Boolean = true,
    //显示时间
    var showDate: Boolean = true,
    //粗体链接
    var urlBold: Boolean = true,

    //是否使用旧版本的UI
    var uiRenderVersionCode: String = "2",

    //ui渲染样式
    var uiStyle: DioRequestUIStyle = DioRequestUIStyle.DefaultStyle

)


/**
 * dio的功能设置
 */
@State(name = "DoxListingUiConfig", storages = [Storage("DoxListingUiConfig.xml")])
class DioxListingUiConfig private constructor() : PersistentStateComponent<DioxListeningSetting> {
    private var config = DioxListeningSetting()
    override fun getState(): DioxListeningSetting {
        return config
    }

    override fun loadState(state: DioxListeningSetting) {
        config = state
    }

    companion object {
        fun getInstance(): PersistentStateComponent<DioxListeningSetting> {
            return service<DioxListingUiConfig>()
        }

        val setting: DioxListeningSetting get() = getInstance().state ?: DioxListeningSetting()

        ///更改设置
        fun changeSetting(doChange: (old: DioxListeningSetting) -> DioxListeningSetting) = getInstance().loadState(doChange(setting))
    }

}