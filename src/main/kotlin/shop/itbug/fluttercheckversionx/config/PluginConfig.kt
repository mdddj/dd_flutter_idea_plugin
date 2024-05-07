package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.*

data class PluginSetting(

    ///是否显示操作工具
    var showRiverpodInlay: Boolean = true
)

@Service
@State(name = "FlutterxFullConfig", storages = [Storage("FlutterxFullConfig.xml")])
class PluginConfig private constructor() : PersistentStateComponent<PluginSetting> {
    private var setting = PluginSetting()
    override fun getState(): PluginSetting {
        return setting
    }

    override fun loadState(state: PluginSetting) {
        setting = state
    }


    companion object {
        private fun getInstance(): PluginConfig {
            return service<PluginConfig>()
        }

        fun getState() = getInstance().state

        fun changeState(change: (old: PluginSetting) -> Unit) {
            getState().apply {
                change(this)
                getInstance().loadState(this)
            }

        }
    }


}