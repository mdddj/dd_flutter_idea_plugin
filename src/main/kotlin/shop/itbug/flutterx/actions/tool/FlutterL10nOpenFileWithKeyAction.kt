package shop.itbug.flutterx.actions.tool

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import shop.itbug.flutterx.services.moveToOffset
import shop.itbug.flutterx.window.l10n.FlutterL10nKeyEditPanel

///在文件中显示这个 key
class FlutterL10nOpenFileWithKeyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editPanel = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? FlutterL10nKeyEditPanel ?: return
        val arbFile = editPanel.arbFile
        val editorArr = FileEditorManager.getInstance(project).openFile(arbFile.file, true)
        if (editorArr.isNotEmpty()) {
            val edit = editorArr.first() as? PsiAwareTextEditorImpl ?: return
            arbFile.moveToOffset(editPanel.key, edit.editor)
        }
    }

    override fun update(e: AnActionEvent) {

        e.presentation.isVisible =
            e.getData(PlatformDataKeys.CONTEXT_COMPONENT) is FlutterL10nKeyEditPanel && e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
