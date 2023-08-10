package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTabbedPane
import shop.itbug.fluttercheckversionx.config.DioxListeningSetting
import shop.itbug.fluttercheckversionx.config.DioxListingUiConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.save.DartFileSaveSettingState
import shop.itbug.fluttercheckversionx.save.dartFileSaveSettingPanel
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig : Configurable, Disposable {

    var model = PluginStateService.getInstance().state ?: AppStateModel()

    private var dioSetting = DioxListingUiConfig.getInstance().state ?: DioxListeningSetting()

    private val generaAssetsSettingPanel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
    private val dartSaveSettingState = DartFileSaveSettingState.getInstance().state

    private var generaAssetsSettingPanelModelIs = false
    private var dartFileSaveSettingPanelModelIs = false

    private var generateSettingPanel =
        GeneraAssetsSettingPanel(settingModel = generaAssetsSettingPanel, parentDisposable = this@AppConfig) {
            generaAssetsSettingPanelModelIs = it
        }

    private var dog = dartFileSaveSettingPanel(this,dartSaveSettingState){
        dartFileSaveSettingPanelModelIs = it
    }

    override fun createComponent(): JComponent {
        return JBTabbedPane().apply {
            add(PluginBundle.get("basic"), panel)
            add(PluginBundle.get("assets.gen"), generateSettingPanel)
            add("保存后执行", dog )
        }
    }

    val dialog: DialogPanel = settingPanel(model, dioSetting, this) {
        model = it
    }

    private val panel: JComponent get() = dialog

    override fun isModified(): Boolean {
        return dialog.isModified() || generaAssetsSettingPanelModelIs || dartFileSaveSettingPanelModelIs
    }

    override fun apply() {
        dialog.apply()
        generateSettingPanel.doApply()
        dog.apply()
        PluginStateService.getInstance().loadState(model)
        DioxListingUiConfig.getInstance().loadState(dioSetting)
        GenerateAssetsClassConfig.getInstance().loadState(generaAssetsSettingPanel)
        DartFileSaveSettingState.getInstance().loadState(dartSaveSettingState)
    }

    override fun getDisplayName(): String {
        return PluginBundle.get("setting")
    }

    override fun reset() {
        dialog.reset()
        dog.reset()
        super.reset()
    }

    override fun dispose() {

    }
}