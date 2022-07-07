package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.vladsch.flexmark.html.HtmlRenderer
import shop.itbug.fluttercheckversionx.document.MarkdownRender
import shop.itbug.fluttercheckversionx.model.example.ResourceModel
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

class ExampleModelDialog(val project: Project, private val exampleModel: ResourceModel): DialogWrapper(project) {
    init {
        title = exampleModel.title
        init()
    }
    override fun createCenterPanel(): JComponent {
        val jpanel = JTextPane()
        jpanel.isEditable = false
        jpanel.contentType = "text/html"
//        jpanel.preferredSize = Dimension(500,800)
        jpanel.text = MarkdownRender.renderText(exampleModel.content,project)
        return JBScrollPane(jpanel)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(400,600)
    }
}