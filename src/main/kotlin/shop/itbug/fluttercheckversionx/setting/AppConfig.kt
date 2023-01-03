package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.options.Configurable
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig : Configurable {

    var model = PluginStateService.getInstance().state ?: AppStateModel()

    override fun createComponent(): JComponent {
        return panel
    }

    private val panel: JComponent  get() = settingPanel(model)

    override fun isModified(): Boolean {
        return true
    }

    override fun apply() {
        println(model)
        PluginStateService.getInstance().loadState(model)
    }

    override fun getDisplayName(): String {
        return PluginBundle.get("setting")
    }

    override fun reset() {
        model = AppStateModel()
        apply()
        super.reset()
    }
}