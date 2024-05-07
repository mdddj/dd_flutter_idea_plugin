package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import javax.swing.JComponent


fun Project.showCodePreviewDialog(code: String) {
    CodeCopyDialog(this, code).show()
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
        title = PluginBundle.get("copy_code_dialog_title")
        super.setOKButtonText("Copy Code")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                scrollCell(editor).align(Align.FILL)
            }
        }
    }


    override fun doOKAction() {
        editor.text.copyTextToClipboard()
        super.doOKAction()
    }
}