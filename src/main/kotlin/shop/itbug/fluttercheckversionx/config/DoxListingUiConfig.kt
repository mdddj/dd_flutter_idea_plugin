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

data class DoxListeningSetting(
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
    var showProjectName: Boolean = true,

    ///项目启动时是否检测flutter新版本
    var checkFlutterVersion: Boolean = true

)


/**
 * dio的功能设置
 */
@State(name = "DoxListingUiConfig", storages = [Storage("DoxListingUiConfig.xml")])
class DioListingUiConfig private constructor() : PersistentStateComponent<DoxListeningSetting> {
    private var config = DoxListeningSetting()
    override fun getState(): DoxListeningSetting {
        return config
    }

    override fun loadState(state: DoxListeningSetting) {
        val old = config
        config = state
        DioSettingChangeEvent.fire(old, state)
    }

    companion object {
        fun getInstance(): PersistentStateComponent<DoxListeningSetting> {
            return service<DioListingUiConfig>()
        }

        val setting: DoxListeningSetting get() = getInstance().state ?: DoxListeningSetting()

        ///更改设置
        fun changeSetting(doChange: (old: DoxListeningSetting) -> DoxListeningSetting) =
            getInstance().loadState(doChange(setting))
    }

}

typealias DioSettingChangeEventChangeFun = (old: DoxListeningSetting, setting: DoxListeningSetting) -> Unit


///当 dio 的一些设置变化后,会发送这个通知事件,可以做一些 UI 上面的更新
interface DioSettingChangeEvent {

    fun doChange(old: DoxListeningSetting, setting: DoxListeningSetting)

    companion object {
        private val TOPIC = Topic.create("DioSettingChangeEvent", DioSettingChangeEvent::class.java)

        ///发送更改事件
        fun fire(old: DoxListeningSetting, setting: DoxListeningSetting) {
            ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).doChange(old, setting)
        }

        ///监听更改事件
        fun listen(call: DioSettingChangeEventChangeFun) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, object : DioSettingChangeEvent {
                override fun doChange(old: DoxListeningSetting, setting: DoxListeningSetting) {
                    call.invoke(old, setting)
                }
            })
        }
    }
}