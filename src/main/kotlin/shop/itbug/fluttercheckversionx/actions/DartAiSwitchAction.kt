package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.util.indexing.FileBasedIndex
import shop.itbug.fluttercheckversionx.common.MyToggleAction
import shop.itbug.fluttercheckversionx.inlay.DartAISetting

/**
 * dart 文件的ai设定
 */
class DartAiSwitchAction : MyToggleAction({ "Display AI operations" }) {

    val setting = DartAISetting.getInstance()

    override fun isSelected(e: AnActionEvent): Boolean {
        return setting.state.showInEditor
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        println("进来了...")
        setting.loadState(setting.state.copy(showInEditor = state))

         val file = FileEditorManager.getInstance(e.project!!).selectedEditor?.file!!

        FileBasedIndex.getInstance().requestReindex(file)
    }


    override fun update(e: AnActionEvent) {
        val file = FileEditorManager.getInstance(e.project!!).selectedEditor?.file
        e.presentation.isEnabled = e.project!=null && file != null
        super.update(e)
    }

    companion object {
        fun getInstance(): AnAction = ActionManager.getInstance().getAction("DartAiSwitchAction")!!
    }
}
