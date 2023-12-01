package shop.itbug.fluttercheckversionx.dialog

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.findPropertiesMaxLenObject
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
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
    }

    override fun createCenterPanel(): JComponent {
        return JBScrollPane(jsonEditView)
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
 * 获取可以转换的 json object
 */
fun String.getParseJsonObject(): JSONObject? {
    if (this.validParseToFreezed()) {
        val isArr = JSON.isValidArray(this)
        if (isArr) {
            val parseArray = JSON.parseArray(this)
            val json = parseArray.findPropertiesMaxLenObject() //属性最多的那一个对象
            return json
        } else {
            val isJson = JSON.isValidObject(this)
            if (isJson) {
                return JSONObject.parse(this)
            }
        }

    }
    return null
}


/**
 * 判断是否可以转换为 freezed
 */
fun String.validParseToFreezed(): Boolean {
    var isJson = JSON.isValidObject(this)
    if (isJson) {
        return true
    }
    isJson = JSON.isValidArray(this)
    return isJson
}