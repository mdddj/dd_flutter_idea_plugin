package shop.itbug.flutterx.hive.action

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


//hive使用文档 // todo 编写使用文档
class HiveTutorialHelp : AnAction() {
    val url = "https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/hive.md"
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(url)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
