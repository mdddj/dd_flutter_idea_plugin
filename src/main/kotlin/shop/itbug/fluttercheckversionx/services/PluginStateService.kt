package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
    name = "flutter-check-x",
    storages = [Storage("flutter-check-x.xml")]
)
class PluginStateService private constructor(): PersistentStateComponent<AppStateModel> {

    private var model = AppStateModel()

    override fun getState(): AppStateModel {
        return model
    }


    override fun loadState(state: AppStateModel) {
        model = state
    }


    companion object {
        fun getInstance(): PersistentStateComponent<AppStateModel> {
            return service<PluginStateService>()
        }

        val appSetting: AppStateModel get() = getInstance().state ?: AppStateModel()
    }
}


///插件全局设置
data class AppStateModel(
    //dio监听端口
    var serverPort: String = "9999",
    //语言设置
    var lang: String = "System",

    //监听到新接口,显示一个提醒
    var apiInToolwindowTop: Boolean = true,

    //选择的房间
//    @OptionTag(converter = ResourceCategoryCovert::class)
//    var selectRoom: ResourceCategory? = null
    ///路径自动补全的触发关键字
    var assetCompilationTriggerString: String = "ass",

    ///路径自动补全的触发文本长度
    var assetCompilationTriggerLen: Int = 3,

    ///路径自动补全扫描目录
    var assetScanFolderName: String = "assets"
)