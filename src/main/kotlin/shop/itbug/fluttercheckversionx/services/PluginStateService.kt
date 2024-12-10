package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.components.*
import java.util.*

@State(
    name = "flutter-check-x",
    storages = [Storage("flutter-check-x.xml")]
)
@Service(Service.Level.APP)
class PluginStateService private constructor() : PersistentStateComponent<AppStateModel> {

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

    ///路径自动补全的触发关键字
    var assetCompilationTriggerString: String = "ass",

    ///路径自动补全的触发文本长度
    var assetCompilationTriggerLen: Int = 3,

    ///路径自动补全扫描目录
    var assetScanFolderName: String = "assets",

    ///是否启用资产自动补全功能
    var assetsScanEnable: Boolean = true,
) {
    /**
     * 获取 flutterx 设置的地区
     */
    fun getSettingLocale(): Locale {
        return when (lang) {
            "System" -> Locale.getDefault()
            "English" -> Locale.US
            "繁體" -> Locale.TRADITIONAL_CHINESE
            "中文" -> Locale.CHINA
            "한국어" -> Locale.KOREA
            "日本語" -> Locale.JAPAN
            else -> Locale.getDefault()
        }
    }
}