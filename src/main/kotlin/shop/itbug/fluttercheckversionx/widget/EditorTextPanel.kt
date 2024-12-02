package shop.itbug.fluttercheckversionx.widget

import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.LanguageTextField
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import shop.itbug.fluttercheckversionx.util.MyFontUtil
import java.awt.Font

private fun LanguageTextField.format(lang: Language) {

    fun createPsiFile(project: Project, text: String): PsiFile {
        val fileName = "tempFile.${lang.id}"
        val psiFileFactory = PsiFileFactory.getInstance(project)
        return psiFileFactory.createFileFromText(fileName, DartLanguage.INSTANCE, text, false, true)
    }

    fun formatCode() {
        val psiFile = createPsiFile(project, text)
        val codeStyleManager = CodeStyleManager.getInstance(project)
        val formattedCode = codeStyleManager.reformat(psiFile)
        text = formattedCode.text
    }
    formatCode()
}

open class JsonEditorTextPanel(project: Project, initText: String = "") :
    LanguageTextField(JsonLanguage.INSTANCE, project, initText, false) {

    init {
        text = initText
        ApplicationManager.getApplication().invokeLater {
            this.format(JsonLanguage.INSTANCE)
        }
    }

    override fun createEditor(): EditorEx {
        return myCreateEditor(super.createEditor())
    }

    fun scrollToTop() {
        ApplicationManager.getApplication().invokeLater {
            editor?.scrollingModel?.scrollVertically(0)
        }
    }


    override fun getFont(): Font = getEditorFont()

}

private fun getEditorFont(): Font = MyFontUtil.getDefaultFont()


class DartEditorTextPanel(project: Project, text: String = "", initFormat: Boolean = true) :
    LanguageTextField(DartLanguage.INSTANCE, project, "", false) {


    init {
        border = emptyBorder()
        this.text = text
        if (initFormat) {
            reformat()
        }
    }

    override fun createEditor(): EditorEx {
        return myCreateEditor(super.createEditor())
    }

    private fun reformat() {
        this.format(DartLanguage.INSTANCE)
    }

    override fun getFont(): Font = getEditorFont()

}


typealias MyCreateEditor = (ex: EditorEx) -> Unit

///创建编辑器
private fun myCreateEditor(ex: EditorEx, init: MyCreateEditor? = null): EditorEx {
    ex.setVerticalScrollbarVisible(true)
    ex.setHorizontalScrollbarVisible(true)
    ex.setBorder(null)
    init?.invoke(ex)
    val settings = ex.settings
    settings.isLineNumbersShown = true
    settings.isAutoCodeFoldingEnabled = true
    settings.isFoldingOutlineShown = true
    settings.isAllowSingleLogicalLineFolding = true
    settings.isRightMarginShown = true
    ex.scrollPane.border = emptyBorder()
    ex.setBorder(emptyBorder())
    return ex
}