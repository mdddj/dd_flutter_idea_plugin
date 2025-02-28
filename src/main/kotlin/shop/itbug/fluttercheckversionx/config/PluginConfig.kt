package shop.itbug.fluttercheckversionx.config

import com.intellij.ide.plugins.RepositoryHelper
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.constance.ideaPluginStoreUrl

data class PluginSetting(

    //是否显示操作工具
    var showRiverpodInlay: Boolean = true,

    //自动导包的定义
    var autoImportRiverpodText: String = "package:hooks_riverpod/hooks_riverpod.dart",

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

    //显示discord
    var showDiscord: Boolean = true,
    //显示qq群
    var showQQGroup: Boolean = true,
    //显示打赏action
    var showRewardAction: Boolean = true,

    //是否加入了开发版本更新
    var isJoinDevVersion: Boolean = RepositoryHelper.getCustomPluginRepositoryHosts().contains(ideaPluginStoreUrl),

    //是否启用 freezed3快速修复面板
    var showFreezed3FixNotification: Boolean = true,
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