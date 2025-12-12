package shop.itbug.flutterx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import kotlinx.serialization.json.Json
import shop.itbug.flutterx.common.jsonToFreezedRun
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.tools.emptyBorder
import shop.itbug.flutterx.widget.JsonEditorTextPanel
import javax.swing.JComponent

/**
 * json转freezed的输入弹窗
 */
class JsonToFreezedInputDialog(val project: Project) : DialogWrapper(project) {
    private val jsonEditView = JsonEditorTextPanel(project)

    init {
        super.init()
        title = PluginBundle.get("input.your.json")
        setSize(500, 500)
        setOKButtonText(PluginBundle.get("freezed.btn.text"))
        jsonEditView.background = this.contentPanel.background
    }

    override fun createCenterPanel(): JComponent {
        return JBScrollPane(jsonEditView).apply {
            border = emptyBorder()
        }
    }


    override fun doOKAction() {
        project.jsonToFreezedRun(jsonEditView.text)
        super.doOKAction()
    }

    override fun doValidate(): ValidationInfo? {
        val text = jsonEditView.text.trim()
        if (text.isEmpty()) {
            return ValidationInfo(PluginBundle.get("input.your.json"), jsonEditView)
        }

        if (text.validParseToFreezed()) {
            return null
        }
        return ValidationInfo(PluginBundle.get("json.format.verification.failed"), jsonEditView)
    }

}


/**
 * 判断是否可以转换为 freezed
 */
fun String.validParseToFreezed(): Boolean {
    return try {
        Json.parseToJsonElement(this)
        true
    } catch (e: Exception) {
        false
    }
}