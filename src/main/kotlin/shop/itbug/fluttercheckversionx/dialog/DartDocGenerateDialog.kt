package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.components.BorderLayoutPanel
import org.intellij.plugins.markdown.ui.preview.MarkdownEditorWithPreview
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.SwingUtil
import shop.itbug.fluttercheckversionx.util.Util
import javax.swing.JComponent


/**
 * dart生成文档弹窗
 */
class DartDocGenerateDialog(override val project: Project): MyDialogWrapper(project) {


    private var mkEditor : MarkdownEditorWithPreview = SwingUtil.getMkEditor(project,"")
    private var prefixTextField = JBTextField("///")

    init {
        super.init()
        title = PluginBundle.get("dart.doc.markdown")
    }

    override fun createCenterPanel(): JComponent {
        return BorderLayoutPanel().apply {
            addToCenter(mkEditor.component)
            addToTop(prefixTextField)
        }
    }

    override fun doOKAction() {
        val text = Util.addStringToLineStart(mkEditor.editor.document.text,prefixTextField.text)
        text.copyTextToClipboard()
        super.doOKAction()
    }
}