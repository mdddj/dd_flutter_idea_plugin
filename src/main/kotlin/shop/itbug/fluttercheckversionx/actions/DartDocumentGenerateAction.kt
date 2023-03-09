package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.DartDocGenerateDialog

class DartDocumentGenerateAction: MyAction({"生成文档"}) {
    override fun actionPerformed(e: AnActionEvent) {
        DartDocGenerateDialog(e.project!!).show()
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        super.update(e)
    }
}