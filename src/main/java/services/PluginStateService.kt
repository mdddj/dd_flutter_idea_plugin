package services

import com.intellij.openapi.components.PersistentStateComponent
import model.PluginVersion

class PluginStateService: PersistentStateComponent<List<PluginVersion>> {

    override fun getState(): List<PluginVersion>? {
        TODO("Not yet implemented")
    }

    override fun loadState(state: List<PluginVersion>) {
        TODO("Not yet implemented")
    }
}