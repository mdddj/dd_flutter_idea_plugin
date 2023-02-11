package shop.itbug.fluttercheckversionx.dialog.jobs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class AddJobsDialog(val project: Project) : DialogWrapper(project) {

    private val mkEdit = LanguageTextField(MarkdownLanguage.INSTANCE, project, "", false)
    private val titleInput = EditorTextField()

    init {
        super.init()
        title = "发布新职位"
    }

    override fun createCenterPanel(): JComponent {
        return object : JPanel(BorderLayout()) {
            init {
                add(titleInput, BorderLayout.NORTH)
                add(JBScrollPane(mkEdit), BorderLayout.CENTER)
            }
        }
    }

}