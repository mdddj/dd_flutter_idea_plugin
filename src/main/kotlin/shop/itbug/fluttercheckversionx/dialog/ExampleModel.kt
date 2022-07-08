package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import shop.itbug.fluttercheckversionx.model.example.ResourceModel
import java.awt.Dimension
import javax.swing.JComponent

class ExampleModelDialog(val project: Project, private val exampleModel: ResourceModel): DialogWrapper(project) {
    init {
        title = exampleModel.title
        init()
    }
    override fun createCenterPanel(): JComponent {
        return JBScrollPane(MarkdownShowComponent(exampleModel.content,project))
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(400,600)
    }
}