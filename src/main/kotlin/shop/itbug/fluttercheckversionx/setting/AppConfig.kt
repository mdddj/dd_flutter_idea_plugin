package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.*
import shop.itbug.fluttercheckversionx.config.*
import shop.itbug.fluttercheckversionx.constance.Links
import shop.itbug.fluttercheckversionx.dialog.MyRowBuild
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.FlutterL10nService
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

//
class AppConfig(val project: Project) : Configurable, SearchableConfigurable {

    var model = PluginStateService.appSetting
    val disposer = Disposer.newDisposable()

    val pluginConfig: PluginSetting = PluginConfig.getState(project)

    private var dioSetting = DioListingUiConfig.getInstance().state ?: DoxListeningSetting()
    private val generaAssetsSettingPanel = GenerateAssetsClassConfig.getGenerateAssetsSetting(project)
    private var generaAssetsSettingPanelModelIs = false
    private var generateSettingPanel =
        GeneraAssetsSettingPanel(
            project,
            settingModel = generaAssetsSettingPanel, parentDisposable = disposer,
        ) {
            generaAssetsSettingPanelModelIs = it
        }


    private lateinit var pluginConfigPanel: DialogPanel

    override fun createComponent(): JComponent {
        pluginConfigPanel = panel {


            group("Riverpod Class Tool") {
                row {
                    checkBox("Enable").bindSelected(pluginConfig::showRiverpodInlay)
                }
                row {
                    textField()
                        .align(Align.FILL)
                        .label(PluginBundle.get("setting_riverpod_import_text_title"), LabelPosition.TOP)
                        .bindText(
                            { pluginConfig.autoImportRiverpodText ?: "" },
                            { pluginConfig.autoImportRiverpodText = it })
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


            group("freezed 3.0 ${PluginBundle.get("tool")}") {
                row {
                    checkBox("Enable").bindSelected(pluginConfig::showFreezed3FixNotification)
                        .comment(PluginBundle.get("freezed3_setting_tooltip"))
                }
            }

            group("Assets Image Preview Window") {
                row {
                    checkBox("Enable").bindSelected(pluginConfig::enableAssetsPreviewAction)
                }
                row("Image Item Width And Height") {
                    intTextField().bindIntText(pluginConfig::assetsPreviewImageSize)
                }
                row("Assets directory") {
                    MyRowBuild.folder(
                        this,
                        { pluginConfig.assetDirectory ?: "" },
                        { pluginConfig.assetDirectory = it },
                        project
                    )
                }
                row {
                    comment(Links.generateDocCommit(Links.assetsPreviewDoc))
                }
            }

            //多语言设置
            group("flutter l10n") {
                row("arb file directory") {
                    MyRowBuild.folder(
                        this,
                        { pluginConfig.l10nFolder ?: "" },
                        { pluginConfig.l10nFolder = it },
                        project
                    )
                }
                row {
                    comment(Links.generateDocCommit(Links.l10nDoc))
                }
            }

            //显示打赏action
            group(PluginBundle.get("reward")) {
                row {
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

    val dialog: DialogPanel = settingPanel(project, model, dioSetting, disposer) {
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
        GenerateAssetsClassConfig.getInstance(project).loadState(generaAssetsSettingPanel)
        PluginConfig.changeState(project) { pluginConfig }
        FlutterL10nService.getInstance(project).configEndTheL10nFolder()
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


    override fun disposeUIResources() {
        Disposer.dispose(disposer)
        println("app config disposed...disposeUIResources()")
    }
}
