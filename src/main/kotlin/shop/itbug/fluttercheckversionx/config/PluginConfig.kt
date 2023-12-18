package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.application.CachedSingletonsRegistry
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import java.util.function.Supplier

data class PluginSetting(
    val showTool: Boolean = false
)

@State(name = "FlutterxConfig", storages = [Storage("FlutterxConfig.xml")])
class PluginConfig : PersistentStateComponent<PluginSetting> {
    private var setting = PluginSetting()
    override fun getState(): PluginSetting {
        return setting
    }

    override fun loadState(state: PluginSetting) {
        setting = state
    }


    companion object {
        val INSTANCESupplier: Supplier<PluginSetting> = CachedSingletonsRegistry.lazy { service<PluginConfig>().state }
    }

}