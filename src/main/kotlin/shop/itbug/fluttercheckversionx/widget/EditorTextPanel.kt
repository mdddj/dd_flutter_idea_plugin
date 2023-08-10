package shop.itbug.fluttercheckversionx.widget

import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.jetbrains.lang.dart.DartLanguage


class JsonEditorTextPanel(project: Project):
    LanguageTextField(JsonLanguage.INSTANCE,project,"",false) {


    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
        val settings = editor.settings
        settings.isLineNumbersShown = true
        settings.isAutoCodeFoldingEnabled = true
        settings.isFoldingOutlineShown = true
        settings.isAllowSingleLogicalLineFolding = true
        settings.isRightMarginShown=true
        return editor
    }
}

class DartEditorTextPanel(project: Project):
    LanguageTextField(DartLanguage.INSTANCE,project,"",false) {


    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
        val settings = editor.settings
        settings.isLineNumbersShown = true
        settings.isAutoCodeFoldingEnabled = true
        settings.isFoldingOutlineShown = true
        settings.isAllowSingleLogicalLineFolding = true
        settings.isRightMarginShown=true
        return editor
    }
}