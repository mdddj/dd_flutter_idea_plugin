package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.save.DartFileSaveSettingState
import shop.itbug.fluttercheckversionx.save.dartFileSaveSettingPanel
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig(val project: Project) : Configurable, Disposable, SearchableConfigurable {


    var model = PluginStateService.getInstance().state ?: AppStateModel()

    val pluginConfig = PluginConfig.getState(project)

    private var dioSetting = DioListingUiConfig.getInstance().state ?: DoxListeningSetting()
    private val generaAssetsSettingPanel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
    private val dartSaveSettingState = DartFileSaveSettingState.getInstance().state
    private var generaAssetsSettingPanelModelIs = false
    private var dartFileSaveSettingPanelModelIs = false
    private var generateSettingPanel =
        GeneraAssetsSettingPanel(settingModel = generaAssetsSettingPanel, parentDisposable = this@AppConfig) {
            generaAssetsSettingPanelModelIs = it
        }

    private var dog = dartFileSaveSettingPanel(this, dartSaveSettingState) {
        dartFileSaveSettingPanelModelIs = it
    }

    private lateinit var pluginConfigPanel: DialogPanel

    override fun createComponent(): JComponent {
        pluginConfigPanel = panel {
            group("Riverpod Class Tool") {
                row {
                    checkBox("Enable").bindSelected(pluginConfig::showRiverpodInlay)
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
        return dialog.isModified() || generaAssetsSettingPanelModelIs || dartFileSaveSettingPanelModelIs
                || pluginConfigPanel.isModified()
    }

    override fun apply() {
        dialog.apply()
        generateSettingPanel.doApply()
        dog.apply()
        pluginConfigPanel.apply()
        PluginStateService.getInstance().loadState(model)
        DioListingUiConfig.getInstance().loadState(dioSetting)
        GenerateAssetsClassConfig.getInstance().loadState(generaAssetsSettingPanel)
        DartFileSaveSettingState.getInstance().loadState(dartSaveSettingState)
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
        dog.reset()
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
