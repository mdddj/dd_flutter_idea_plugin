package shop.itbug.fluttercheckversionx.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


class FlutterXRunOptionEditor(val factory: FlutterXRunConfiguration) : SettingsEditor<FlutterXRunConfiguration>() {

    val state: FlutterXRunConfigOptions = factory.getMyOptions();

    private val myPanel = panel {
        row("Run Flutter App") {
            textField().bindText(state::script)
        }
    }

    override fun resetEditorFrom(s: FlutterXRunConfiguration) {
        s.setNewOptions(s.getMyOptions())
    }

    override fun applyEditorTo(s: FlutterXRunConfiguration) {
        myPanel.apply()
        s.setNewOptions(state)
    }

    override fun createEditor(): JComponent {
        return myPanel
    }
}


