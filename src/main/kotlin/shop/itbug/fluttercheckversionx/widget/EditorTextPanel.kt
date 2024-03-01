package shop.itbug.fluttercheckversionx.widget

import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.LanguageTextField
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.util.reformatText
import javax.swing.BorderFactory


open class JsonEditorTextPanel(project: Project) : LanguageTextField(JsonLanguage.INSTANCE, project, "", false) {


    override fun createEditor(): EditorEx {
        return myCreateEditor(super.createEditor())
    }


    fun scrollToTop() {
        editor?.scrollingModel?.scrollVertically(0)
    }
}

class DartEditorTextPanel(project: Project, text: String = "") :
    LanguageTextField(DartLanguage.INSTANCE, project, "", false) {



    init {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        this.text = text
    }

    override fun createEditor(): EditorEx {
        return myCreateEditor(super.createEditor())
    }

    fun reformat() {
        val instance = PsiDocumentManager.getInstance(project)
        val psiFile = instance.getPsiFile(document)
        psiFile?.reformatText()
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
    return ex
}