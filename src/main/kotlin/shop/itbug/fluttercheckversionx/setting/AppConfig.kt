package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import shop.itbug.fluttercheckversionx.config.DioxListeningSetting
import shop.itbug.fluttercheckversionx.config.DioxListingUiConfig
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig : Configurable, Disposable {

    var model = PluginStateService.getInstance().state ?: AppStateModel()

    private var dioSetting = DioxListingUiConfig.getInstance().state ?: DioxListeningSetting()

    override fun createComponent(): JComponent {
        return panel
    }

    val dialog: DialogPanel = settingPanel(model, dioSetting,this) {
        model = it
    }
    private val panel: JComponent get() = dialog

    override fun isModified(): Boolean {
        return dialog.isModified()
    }

    override fun apply() {
        dialog.apply()
        PluginStateService.getInstance().loadState(model)
        DioxListingUiConfig.getInstance().loadState(dioSetting)
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