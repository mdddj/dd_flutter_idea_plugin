package shop.itbug.fluttercheckversionx.config

import com.alibaba.fastjson2.toJSONString
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

data class PluginSetting(

    ///是否显示操作工具
    val showRiverpodInlay: Boolean = true
)

@State(name = "FlutterxConfig", storages = [Storage("FlutterxConfig.xml")])
class PluginConfig : PersistentStateComponent<PluginSetting> {
    private var setting = PluginSetting()
    override fun getState(): PluginSetting {
        return setting
    }

    override fun loadState(state: PluginSetting) {
        setting = state
        println("更新${setting.toJSONString()}")
    }


    companion object {
        fun getInstance(): PluginConfig {
            return service<PluginConfig>()
        }

        fun getState() = getInstance().state
    }


}