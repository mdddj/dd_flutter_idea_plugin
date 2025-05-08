package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class MyTextDialog(
    project: Project, val label: String,
    val comment: String? = null,
    val initValue: String? = null,
    val handle: (text: String) -> Unit
) :
    DialogWrapper(project, true) {
    lateinit var dialogPanel: DialogPanel
    var text = initValue ?: ""

    init {
        super.init()
        title = "FlutterX"
    }

    override fun createCenterPanel(): JComponent? {
        dialogPanel = panel {
            row {
                textField().bindText({ text }, {
                    text = it
                })
                    .label(label, LabelPosition.TOP).comment(comment).component.apply {
                        requestFocus()
                    }
            }
        }
        return dialogPanel
    }

    override fun doOKAction() {
        dialogPanel.apply()
        if (text.isNotEmpty()) {
            handle(text)
        }
        super.doOKAction()

    }
}