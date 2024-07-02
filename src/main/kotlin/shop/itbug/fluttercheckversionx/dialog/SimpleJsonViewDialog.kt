package shop.itbug.fluttercheckversionx.dialog

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import java.awt.Dimension
import javax.swing.JComponent

///简单的一个json查看弹窗
class SimpleJsonViewDialog(jsonObject: Any, project: Project) : DialogWrapper(project) {
    private val initText: String = Gson().toJson(jsonObject)

    private var jsonView = JsonEditorTextPanel(project, initText)

    init {
        init()
        jsonView.background = this.contentPanel.background
        title = PluginBundle.get("json.viewer")
    }

    override fun createCenterPanel(): JComponent {
        return jsonView
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(500, super.getPreferredSize().height)
    }

    override fun getSize(): Dimension {
        return preferredSize
    }

    companion object {
        fun show(jsonObject: Any, project: Project) {
            SimpleJsonViewDialog(jsonObject, project).show()
        }
    }
}