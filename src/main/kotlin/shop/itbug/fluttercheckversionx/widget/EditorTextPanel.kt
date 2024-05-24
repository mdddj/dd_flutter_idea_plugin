package shop.itbug.fluttercheckversionx.widget

import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.LanguageTextField
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import java.awt.Font


open class JsonEditorTextPanel(project: Project, initText: String = "") :
    LanguageTextField(JsonLanguage.INSTANCE, project, initText, false) {


    override fun createEditor(): EditorEx {
        return myCreateEditor(super.createEditor())
    }

    fun scrollToTop() {
        editor?.scrollingModel?.scrollVertically(0)
    }

    override fun getFont(): Font {
        return EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
    }
}

class DartEditorTextPanel(project: Project, text: String = "") :
    LanguageTextField(DartLanguage.INSTANCE, project, "", false) {


    init {
        border = emptyBorder()
        this.text = text
        reformat()

    }

    override fun createEditor(): EditorEx {
        return myCreateEditor(super.createEditor())
    }

    private fun reformat() {
        formatCode()
    }

    override fun getFont(): Font {
        return EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
    }

    private fun formatCode() {

        // Create a PsiFile from the text
        val psiFile = createPsiFile(project, text)

        // Get CodeStyleManager instance
        val codeStyleManager = CodeStyleManager.getInstance(project)

        // Reformat the code
        val formattedCode = codeStyleManager.reformat(psiFile)

        // Update the text field with the formatted code
        text = formattedCode.text
    }

    private fun createPsiFile(project: Project, text: String): PsiFile {
        val fileName = "tempFile.${DartLanguage.INSTANCE.id}"
        val psiFileFactory = PsiFileFactory.getInstance(project)
        return psiFileFactory.createFileFromText(fileName, DartLanguage.INSTANCE, text, false, true)
    }
}


///创建编辑器
private fun myCreateEditor(ex: EditorEx): EditorEx {
    ex.setVerticalScrollbarVisible(true)
    ex.setHorizontalScrollbarVisible(true)
    ex.setBorder(null)
    val settings = ex.settings
    settings.isLineNumbersShown = true
    settings.isAutoCodeFoldingEnabled = true
    settings.isFoldingOutlineShown = true
    settings.isAllowSingleLogicalLineFolding = true
    settings.isRightMarginShown = true
    ex.scrollPane.border = emptyBorder()
    return ex
}