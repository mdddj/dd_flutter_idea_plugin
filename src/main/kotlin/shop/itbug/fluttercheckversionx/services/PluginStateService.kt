package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
    name = "flutter-check-x",
    storages = [Storage("flutter-check-x.xml")]
)
class PluginStateService : PersistentStateComponent<AppStateModel> {

    private var model = AppStateModel()

    override fun getState(): AppStateModel {
        return model
    }

    override fun loadState(state: AppStateModel) {
        model = state
    }

    val setting get() = state

    companion object {
        fun getInstance(): PersistentStateComponent<AppStateModel> {
            return service<PluginStateService>()
        }
    }
}

///插件全局设置
data class AppStateModel(var serverPort: String = "9999", var lang: String = "System")