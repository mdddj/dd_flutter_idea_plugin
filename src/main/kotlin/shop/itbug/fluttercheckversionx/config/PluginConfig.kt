package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

data class PluginSetting(

    //是否显示操作工具
    var showRiverpodInlay: Boolean = true,
    //在Android studio中打开android目录
    var openAndroidDirectoryInAS: Boolean = true,
    //在xcode中打开ios目录
    var openIosDirectoryInXcode: Boolean = true,
    //在xcode中打开macos目录
    var openMacosDirectoryInXcode: Boolean = true,

    //开启本地资产预览
    var showAssetsIconInEditor: Boolean = true,
    //缩放大小
    var assetsIconSize: Int = 16,
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