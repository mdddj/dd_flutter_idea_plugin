package shop.itbug.fluttercheckversionx.actions.run

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FlutterXRunHotRefresh : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("hot refresh")
    }
}
