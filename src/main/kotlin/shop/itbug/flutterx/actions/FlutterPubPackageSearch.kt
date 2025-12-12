package shop.itbug.flutterx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.common.MyAction
import shop.itbug.flutterx.util.MyActionUtil

class FlutterPubPackageSearch : MyAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { MyActionUtil.showPubSearchDialog(it) }
    }
}