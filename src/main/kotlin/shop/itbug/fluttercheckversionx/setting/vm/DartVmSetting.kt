package shop.itbug.fluttercheckversionx.setting.vm

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.widget.PopupWidgets
import shop.itbug.fluttercheckversionx.window.getDartVmWindow
import javax.swing.JComponent

class DartVmSetting(val project: Project) : Configurable {
    private lateinit var myPanel: DialogPanel
    private val state = PluginConfig.getState(project)

    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return "Dart VM Settings"
    }

    override fun createComponent(): JComponent {
        myPanel = panel {
            row {
                checkBox("Enable Dart VM tool window").bindSelected(state::enableVmServiceToolWindow)
                    .comment("(only this project)")
            }
            row {
                checkBox("Listen to vm service connection").bindSelected(state::enableVmServiceListen)
                    .comment("Disabled, vm service related services are unavailable (only this project) ")
            }
        }
        return myPanel
    }

    override fun isModified(): Boolean {
        return myPanel.isModified()
    }

    override fun apply() {
        myPanel.apply()
        PluginConfig.getState(project).enableVmServiceToolWindow = state.enableVmServiceToolWindow
        PluginConfig.getState(project).enableVmServiceListen = state.enableVmServiceListen
        FlutterXVMService.getInstance(project).settingChanged()

    }

    override fun disposeUIResources() {
        if (!state.enableVmServiceToolWindow) {
            project.getDartVmWindow()?.let {
                it.hide()
                it.isAvailable = false
            }
        }else {
            project.getDartVmWindow()?.let {
                it.show()
                it.isAvailable = true
            }
        }
        if(!state.enableVmServiceListen){
            PopupWidgets.restartWithConfirmation()
        }
        super.disposeUIResources()
    }
}