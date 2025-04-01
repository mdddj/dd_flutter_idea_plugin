package shop.itbug.fluttercheckversionx.actions.test

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.manager.PluginChangelogCache

//测试新版本弹窗
class TestActionWithShowNewVersionPopup : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            PluginChangelogCache.getInstance().testShow(it)
        }
    }
}