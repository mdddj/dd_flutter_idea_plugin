package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.*
import shop.itbug.fluttercheckversionx.config.*
import shop.itbug.fluttercheckversionx.constance.Links
import shop.itbug.fluttercheckversionx.constance.ideaPluginStoreUrl
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig(val project: Project) : Configurable, Disposable, SearchableConfigurable {

    var model = PluginStateService.appSetting

    val pluginConfig: PluginSetting = PluginConfig.getState(project)

    private var dioSetting = DioListingUiConfig.getInstance().state ?: DoxListeningSetting()
    private val generaAssetsSettingPanel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
    private var generaAssetsSettingPanelModelIs = false
    private var generateSettingPanel =
        GeneraAssetsSettingPanel(
            settingModel = generaAssetsSettingPanel, parentDisposable = this@AppConfig
        ) {
            generaAssetsSettingPanelModelIs = it
        }


    private lateinit var pluginConfigPanel: DialogPanel

    override fun createComponent(): JComponent {
        pluginConfigPanel = panel {

            group(PluginBundle.get("use_dev_version_plugin_title")) {
                row {
                    checkBox(PluginBundle.get("use_dev_version_plugin")).bindSelected(pluginConfig::isJoinDevVersion)
                        .onChanged {
                            if (it.isSelected) {
                                UpdateSettings.getInstance().storedPluginHosts.add(ideaPluginStoreUrl)
                            } else {
                                UpdateSettings.getInstance().storedPluginHosts.remove(ideaPluginStoreUrl)
                            }
                        }
                }
            }

            group("Riverpod Class Tool") {
                row {
                    checkBox("Enable").bindSelected(pluginConfig::showRiverpodInlay)
                }
                row {
                    textField()
                        .align(Align.FILL)
                        .label(PluginBundle.get("setting_riverpod_import_text_title"), LabelPosition.TOP)
                        .bindText(pluginConfig::autoImportRiverpodText)
                }
                row {
                    comment(Links.generateDocCommit(Links.riverpod))
                }
            }
            group("Open Project in Ide") {
                row("Open the android directory in Android Studio") {
                    checkBox("Enable").bindSelected(pluginConfig::openAndroidDirectoryInAS)
                }
                row("Open the ios directory in Xcode") {
                    checkBox("Enable").bindSelected(pluginConfig::openIosDirectoryInXcode)
                }
                row("Open the macos directory in Xcode") {
                    checkBox("Enable").bindSelected(pluginConfig::openMacosDirectoryInXcode)
                }
                row {
                    comment(Links.generateDocCommit(Links.openIn))
                }
            }

            group(PluginBundle.get("foot_bar_links_title")) {
                row(PluginBundle.get("setting_show_discord_action")) {
                    checkBox("Enable").bindSelected(pluginConfig::showDiscord)
                }
                row(PluginBundle.get("setting_show_qq_group_action")) {
                    checkBox("Enable").bindSelected(pluginConfig::showQQGroup)
                }
                row {
                    comment(Links.generateDocCommit(Links.link))
                }
            }
            group("Assets Icon") {
                row(PluginBundle.get("assets_icon_enable_in_editor")) {
                    checkBox("Enable").bindSelected(pluginConfig::showAssetsIconInEditor)
                }
                row(PluginBundle.get("assets_icon_scale_size")) {
                    intTextField().bindIntText(pluginConfig::assetsIconSize)
                }
                row {
                    comment(Links.generateDocCommit(Links.icons))
                }

            }

            //显示打赏action
            group(PluginBundle.get("reward")) {
                row("Enable") {
                    checkBox(PluginBundle.get("setting_show_reward_action")).bindSelected(pluginConfig::showRewardAction)
                        .comment(PluginBundle.get("setting_show_reward_action_tip"))
                }
            }


        }
        return JBTabbedPane().apply {
            add(PluginBundle.get("basic"), panel)
            add(PluginBundle.get("assets.gen"), generateSettingPanel)
            add("FlutterX", pluginConfigPanel)
        }
    }

    val dialog: DialogPanel = settingPanel(model, dioSetting, this) {
        model = it
    }

    private val panel: JComponent get() = dialog

    override fun isModified(): Boolean {
        return dialog.isModified() || generaAssetsSettingPanelModelIs
                || pluginConfigPanel.isModified()
    }

    override fun apply() {
        dialog.apply()
        generateSettingPanel.doApply()
        pluginConfigPanel.apply()
        PluginStateService.getInstance().loadState(model)
        DioListingUiConfig.getInstance().loadState(dioSetting)
        GenerateAssetsClassConfig.getInstance().loadState(generaAssetsSettingPanel)
        PluginConfig.changeState(project) { pluginConfig }
    }

    override fun getDisplayName(): String {
        return PluginBundle.get("setting.flutterx")
    }

    override fun getId(): String {
        return "flutterx"
    }

    override fun reset() {
        dialog.reset()
        super<Configurable>.reset()
        pluginConfigPanel.reset()
    }

    override fun dispose() {

    }


    override fun disposeUIResources() {
        Disposer.dispose(this)
        super<Configurable>.disposeUIResources()
    }
}
