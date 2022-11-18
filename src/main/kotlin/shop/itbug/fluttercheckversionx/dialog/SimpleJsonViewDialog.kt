package shop.itbug.fluttercheckversionx.dialog

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import javax.swing.JComponent

///简单的一个json查看弹窗
class SimpleJsonViewDialog(jsonObject: Any, project: Project) : DialogWrapper(project) {

    private var jsonView: LanguageTextField = LanguageTextField(
        JsonLanguage.INSTANCE,
        project,
        JSONObject.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat),
        false
    )

    init {
        init()
        title = "JSON查看器"
    }

    override fun createCenterPanel(): JComponent {
        return jsonView
    }

    companion object {
        fun show(jsonObject: Any,project: Project) {
            SimpleJsonViewDialog(jsonObject, project).show()
        }
    }
}