package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.SearchDialog

class FlutterPubPackageSearch : MyAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { SearchDialog(it).show() }
    }
}