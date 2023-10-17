package shop.itbug.fluttercheckversionx.hive.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


//hive使用文档 // todo 编写使用文档
class HiveTutorialHelp : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("前往文档")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
