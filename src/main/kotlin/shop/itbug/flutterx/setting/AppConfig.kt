package shop.itbug.flutterx.setting

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.EditorNotifications
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.*
import icons.MyImages
import shop.itbug.flutterx.actions.context.SiteDocument
import shop.itbug.flutterx.config.*
import shop.itbug.flutterx.constance.Links
import shop.itbug.flutterx.dialog.MyRowBuild
import shop.itbug.flutterx.dsl.settingPanel
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.services.FlutterL10nService
import shop.itbug.flutterx.services.MyUserBarFactory
import shop.itbug.flutterx.services.PluginStateService
import shop.itbug.flutterx.socket.service.DioApiService
import javax.swing.JComponent

//
class AppConfig(val project: Project) : Configurable, SearchableConfigurable {
    private val logger = thisLogger()
    var model = PluginStateService.appSetting
    val disposer = Disposer.newDisposable()

    val pluginConfig: PluginSetting = PluginConfig.getState(project)
    val globalConfig = FlutterXGlobalConfigService.getInstance()

    private val dioSetting get() = DioListingUiConfig.getInstance().state ?: DoxListeningSetting()
    private val initDioSetting = dioSetting.copy()
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


            group(PluginBundle.get("app.config.riverpod.group")) {
                row {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::showRiverpodInlay)
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
                    comment(Links.generateDocCommit(Links.RIVERPOD))
                }
            }
            group(PluginBundle.get("app.config.open.project.group")) {
                row(PluginBundle.get("app.config.open.android.studio")) {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::openAndroidDirectoryInAS)
                }
                row(PluginBundle.get("app.config.open.ios.xcode")) {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::openIosDirectoryInXcode)
                }
                row(PluginBundle.get("app.config.open.macos.xcode")) {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::openMacosDirectoryInXcode)
                }
                row {
                    comment(Links.generateDocCommit(Links.OPEN_IN))
                }
            }

            group(PluginBundle.get("foot_bar_links_title")) {
                row(PluginBundle.get("setting_show_discord_action")) {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::showDiscord)
                }
                row(PluginBundle.get("setting_show_qq_group_action")) {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::showQQGroup)
                }
                row {
                    comment(Links.generateDocCommit(Links.LINK))
                }
            }
            group(PluginBundle.get("app.config.assets.icon.group")) {
                row(PluginBundle.get("assets_icon_enable_in_editor")) {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::showAssetsIconInEditor)
                }
                row(PluginBundle.get("assets_icon_scale_size")) {
                    intTextField().bindIntText(pluginConfig::assetsIconSize)
                }
                row {
                    comment(Links.generateDocCommit(Links.ICON))
                }

            }


            group(PluginBundle.get("app.config.freezed.notifications.group", PluginBundle.get("tool"))) {
                row {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::showFreezed3FixNotification)
                        .comment(PluginBundle.get("freezed3_setting_tooltip"))
                }
            }

            group(PluginBundle.get("app.config.pubspec.notifications.group")) {
                row {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::showPubspecYamlNotificationBar)
                        .comment(PluginBundle.get("pubspec_notification_bar_tooltip"))
                }
            }

            group(PluginBundle.get("app.config.assets.preview.group")) {
                row {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::enableAssetsPreviewAction)
                }
                row(PluginBundle.get("app.config.assets.preview.image.size")) {
                    intTextField().bindIntText(pluginConfig::assetsPreviewImageSize)
                }
                row(PluginBundle.get("app.config.assets.preview.directory")) {
                    MyRowBuild.folder(
                        this,
                        { pluginConfig.assetDirectory ?: "" },
                        { pluginConfig.assetDirectory = it },
                        project
                    )
                }
                row {
                    comment(Links.generateDocCommit(SiteDocument.AssetsPreview.url))
                }
            }

            group(PluginBundle.get("app.config.l10n.group")) {
                row {
                    checkBox(PluginBundle.get("l10n.scan.dart.string.setting")).bindSelected(pluginConfig::scanDartStringInStart)
                }
                row(PluginBundle.get("app.config.l10n.arb.directory")) {
                    MyRowBuild.folder(
                        this,
                        { pluginConfig.l10nFolder ?: "" },
                        { pluginConfig.l10nFolder = it },
                        project
                    )
                }
                row {
                    comment(Links.generateDocCommit(Links.L10N_DOC))
                }
            }

            group(PluginBundle.get("app.config.freezed.intention.group")) {
                row {
                    checkBox(PluginBundle.get("open")).bindSelected(pluginConfig::enableFreezedIntentionActions)
                }
            }

            group(PluginBundle.get("reward")) {
                row {
                    checkBox(PluginBundle.get("setting_show_reward_action")).bindSelected(pluginConfig::showRewardAction)
                        .comment(PluginBundle.get("setting_show_reward_action_tip"))
                }
            }

            group(PluginBundle.get("app.config.status.bar.group")) {
                row {
                    checkBox(PluginBundle.get("app.config.status.bar.force.show")).bindSelected(
                        { globalConfig.state.forceEnableBottomStatusBarActions },
                        {
                            globalConfig.state.forceEnableBottomStatusBarActions = it
                        })
                }
                row {
                    checkBox(PluginBundle.get("app.config.status.bar.force.hide")).bindSelected({
                        globalConfig.state.forceHideBottomStatusBarAction
                    }, {
                        logger.info(PluginBundle.get("app.config.status.bar.force.hide.log"))
                        globalConfig.state.forceHideBottomStatusBarAction = it

                    })
                }
                row {
                    cell(JBLabel(MyImages.load("/images/status_bar_img.png")))
                }
                row {
                    comment(PluginBundle.get("app.config.status.bar.comment"))
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
        EditorNotifications.getInstance(project).updateAllNotifications()
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

    override fun cancel() {
        super<SearchableConfigurable>.cancel()
        logger.info("flutterx config canceled")
        tryHandleDioSettings()
        tryHandleStatusBarStatus()
    }

    private fun tryHandleDioSettings() {
        if (initDioSetting.enableFlutterXDioSocket != dioSetting.enableFlutterXDioSocket) {
            if (!dioSetting.enableFlutterXDioSocket) {
                DioApiService.getInstance().stopAll(project)
            } else {
                DioApiService.getInstance().tryStart(project)
            }
        }
    }

    override fun disposeUIResources() {
        Disposer.dispose(disposer)
        println("app config disposed...disposeUIResources()")
        tryHandleDioSettings()
        tryHandleStatusBarStatus()
    }

    private fun tryHandleStatusBarStatus(){
        WindowManager.getInstance().getStatusBar(project).updateWidget(MyUserBarFactory.ID)
    }
}
