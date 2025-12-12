package shop.itbug.flutterx.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class PluginSetting : BaseState() {
    var showRiverpodInlay by property(true)
    var autoImportRiverpodText by string("package:hooks_riverpod/hooks_riverpod.dart")
    var openAndroidDirectoryInAS by property(true)
    var openIosDirectoryInXcode by property(true)
    var openMacosDirectoryInXcode by property(true)
    var showAssetsIconInEditor by property(true)
    var assetsIconSize by property(16)
    var showDiscord by property(true)
    var showQQGroup by property(true)
    var showRewardAction by property(true)
    var showFreezed3FixNotification by property(true)
    var enableAssetsPreviewAction by property(true)
    var assetsPreviewImageSize by property(120)
    var assetDirectory by string()
    var l10nFolder by string("") //flutter多语言 arb 目录
    var l10nDefaultFileName by string("") // 默认多语言文件名
    var scanDartStringInStart by property(true) //启动时扫描字符串

    //启动 flutterx vm 窗口
    var enableVmServiceToolWindow by property(true)

    //启用dart vm service 控制台监听器
    var enableVmServiceListen by property(true)

    //启用 freezed 工具
    var enableFreezedIntentionActions by property(false)


}

@Service(Service.Level.PROJECT)
@State(name = "FlutterxFullConfig", storages = [Storage("FlutterxFullConfig.xml")])
class PluginConfig(val project: Project) : SimplePersistentStateComponent<PluginSetting>(PluginSetting()) {


    fun initAssetsDirectory() {
        if (state.assetDirectory.isNullOrBlank()) {
            state.assetDirectory =
                ApplicationManager.getApplication().executeOnPooledThread<String> {
                    (project.guessProjectDir()?.findChild("assets")?.path) ?: project.guessProjectDir()?.path
                }.get()
            loadState(state)
        }
    }

    fun initL10nFolder() {
        if (state.l10nFolder.isNullOrBlank()) {
            state.l10nFolder =
                ApplicationManager.getApplication().executeOnPooledThread<String> {
                    project.guessProjectDir()?.findChild("lib")?.findChild("l10n")?.path ?: (project.guessProjectDir()
                        ?.findChild("lib")?.path ?: project.guessProjectDir()?.path)
                }.get()
            loadState(state)
        }
    }


    companion object {
        fun getInstance(project: Project): PluginConfig {
            val config = project.service<PluginConfig>()
            config.initAssetsDirectory()
            config.initL10nFolder()
            return config
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