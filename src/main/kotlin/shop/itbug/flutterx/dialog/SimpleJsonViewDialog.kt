package shop.itbug.flutterx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.util.getJsonString
import shop.itbug.flutterx.widget.JsonEditorTextPanel
import java.awt.Dimension
import javax.swing.JComponent

///简单的一个json查看弹窗
class SimpleJsonViewDialog(jsonObject: Any, project: Project) : DialogWrapper(project) {
    private val initText: String = getJsonString(jsonObject)

    private var jsonView = JsonEditorTextPanel(project, initText)

    init {
        super.init()
        jsonView.background = this.contentPanel.background
        title = PluginBundle.get("json.viewer")
    }

    override fun createCenterPanel(): JComponent {
        return jsonView
    }


    override fun getSize(): Dimension {
        return Dimension(500, 500)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(500, 500)
    }

    companion object {
        fun show(jsonObject: Any, project: Project) {
            SimpleJsonViewDialog(jsonObject, project).show()
        }
    }
}