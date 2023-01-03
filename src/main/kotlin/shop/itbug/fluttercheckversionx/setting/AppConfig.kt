package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.tools.MyToolWindowTools
import javax.swing.JComponent

class AppConfig : Configurable, Disposable {

    var model = PluginStateService.getInstance().state ?: AppStateModel()

    override fun createComponent(): JComponent {
        return panel
    }

    val dialog: DialogPanel = settingPanel(model, this) {
        model = it
    }
    private val panel: JComponent get() = dialog

    override fun isModified(): Boolean {
        return dialog.isModified()
    }

    override fun apply() {
        dialog.apply()
        println("appled: $model")
        PluginStateService.getInstance().loadState(model)
       val project =  ProjectManager.getInstance().defaultProject
        MyToolWindowTools.getMyToolWindow(project)?.apply {
            reset()
        }
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