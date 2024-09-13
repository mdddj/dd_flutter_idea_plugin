package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

data class PluginSetting(

    ///是否显示操作工具
    var showRiverpodInlay: Boolean = true
) : BaseState()

@Service(Service.Level.PROJECT)
@State(name = "FlutterxFullConfig", storages = [Storage("FlutterxFullConfig.xml")])
class PluginConfig : SimplePersistentStateComponent<PluginSetting>(PluginSetting()) {
    private var setting = PluginSetting()

    override fun loadState(state: PluginSetting) {
        setting = state
    }

    companion object {
        fun getInstance(project: Project): PluginConfig {
            return project.service<PluginConfig>()
        }

        fun getState(project: Project) = getInstance(project).state

        fun changeState(project: Project, change: (old: PluginSetting) -> Unit) {
            getState(project).apply {
                change(this)
                getInstance(project).loadState(this)
            }

        }
    }


}