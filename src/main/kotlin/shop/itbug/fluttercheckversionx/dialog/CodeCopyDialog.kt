package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import javax.swing.JComponent


fun Project.showCodePreviewDialog(code: String) {
    CodeCopyDialog(this,code).show()
}

class CodeCopyDialog(override val project: Project, val code: String) : MyDialogWrapper(project) {

    val editor = LanguageTextField(
        PlainTextLanguage.INSTANCE,
        project,
        code,
        false

    )

    init {
        super.init()
        title = "Copy code dialog"
        super.setOKButtonText("Copy Code")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                scrollCell(editor).horizontalAlign(HorizontalAlign.FILL)
            }
        }
    }


    override fun doOKAction() {
        editor.text.copyTextToClipboard()
        super.doOKAction()
    }
}