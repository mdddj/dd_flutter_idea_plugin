package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.i18n.PluginBundle


///ui 渲染样式
enum class DioRequestUIStyle(val string: String) {
    //默认样式 (宽松)
    DefaultStyle(PluginBundle.get("relaxed.mode")),

    //紧凑模式
    CompactStyle(PluginBundle.get("compact.mode"))
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
    var uiStyle: DioRequestUIStyle = DioRequestUIStyle.DefaultStyle,

    ///是否自动滚动到地步
    var autoScroller: Boolean = true,

    ///显示项目名字
    var showProjectName: Boolean = true

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
        val old = config
        config = state
        DioSettingChangeEvent.fire(old, state)
    }

    companion object {
        fun getInstance(): PersistentStateComponent<DioxListeningSetting> {
            return service<DioxListingUiConfig>()
        }

        val setting: DioxListeningSetting get() = getInstance().state ?: DioxListeningSetting()

        ///更改设置
        fun changeSetting(doChange: (old: DioxListeningSetting) -> DioxListeningSetting) =
            getInstance().loadState(doChange(setting))
    }

}

typealias DioSettingChangeEventChangeFun = (old: DioxListeningSetting, setting: DioxListeningSetting) -> Unit


///当 dio 的一些设置变化后,会发送这个通知事件,可以做一些 UI 上面的更新
interface DioSettingChangeEvent {

    fun doChange(old: DioxListeningSetting, setting: DioxListeningSetting)

    companion object {
        private val TOPIC = Topic.create("DioSettingChangeEvent", DioSettingChangeEvent::class.java)

        ///发送更改事件
        fun fire(old: DioxListeningSetting, setting: DioxListeningSetting) {
            ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).doChange(old, setting)
        }

        ///监听更改事件
        fun listen(call: DioSettingChangeEventChangeFun) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, object : DioSettingChangeEvent {
                override fun doChange(old: DioxListeningSetting, setting: DioxListeningSetting) {
                    call.invoke(old, setting)
                }
            })
        }
    }
}