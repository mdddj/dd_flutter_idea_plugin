package shop.itbug.fluttercheckversionx.dialog

import com.alibaba.fastjson2.JSONObject
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import javax.swing.JComponent

class JsonToFreezedInputDialog(val project: Project) : DialogWrapper(project) {
    private val jsonEditView = LanguageTextField(
        JsonLanguage.INSTANCE,
        project,
        "",
        false
    )

    init {
        super.init()
        title = PluginBundle.get("input.your.json")
        setSize(500,500)
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
        if(jsonEditView.text.trim().isEmpty()){
            return ValidationInfo(PluginBundle.get("input.your.json"),jsonEditView)
        }
        try {
            JSONObject.parseObject(jsonEditView.text)
        }catch (e: Exception){
            return ValidationInfo(PluginBundle.get("json.format.verification.failed"),jsonEditView)
        }
        return super.doValidate()
    }


}