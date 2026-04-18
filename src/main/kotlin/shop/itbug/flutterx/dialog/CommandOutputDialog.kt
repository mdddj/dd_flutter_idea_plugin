package shop.itbug.flutterx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class CommandOutputDialog(
    project: Project,
    private val dialogTitle: String,
    private val output: String
) : DialogWrapper(project, true) {

    init {
        title = dialogTitle
        init()
    }

    override fun createCenterPanel(): JComponent {
        val textArea = JBTextArea(output).apply {
            isEditable = false
            lineWrap = false
            wrapStyleWord = false
            caretPosition = 0
        }

        return JBScrollPane(textArea).apply {
            preferredSize = JBUI.size(900, 520)
        }
    }
}
