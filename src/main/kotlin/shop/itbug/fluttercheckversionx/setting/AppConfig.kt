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
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig : Configurable, Disposable {

    var model = PluginStateService.getInstance().state ?: AppStateModel()

    private var dioSetting = DioxListingUiConfig.getInstance().state ?: DioxListeningSetting()

    private val generaAssetsSettingPanel = GenerateAssetsClassConfig.getGenerateAssetsSetting()

    private var generaAssetsSettingPanelModelIs = false

    private var generateSettingPanel =
        GeneraAssetsSettingPanel(settingModel = generaAssetsSettingPanel, parentDisposable = this@AppConfig) {
            generaAssetsSettingPanelModelIs = it
        }

    override fun createComponent(): JComponent {
        return JBTabbedPane().apply {
            add("基本", panel)
            add("资产生成", generateSettingPanel)
        }
    }

    val dialog: DialogPanel = settingPanel(model, dioSetting, this) {
        model = it
    }

    private val panel: JComponent get() = dialog

    override fun isModified(): Boolean {
        return dialog.isModified() || generaAssetsSettingPanelModelIs
    }

    override fun apply() {
        dialog.apply()
        generateSettingPanel.doApply()
        PluginStateService.getInstance().loadState(model)
        DioxListingUiConfig.getInstance().loadState(dioSetting)
        GenerateAssetsClassConfig.getInstance().loadState(generaAssetsSettingPanel)
    }

    override fun getDisplayName(): String {
        return PluginBundle.get("setting")
    }

    override fun reset() {
        dialog.reset()
        super.reset()
    }

    override fun dispose() {

    }
}