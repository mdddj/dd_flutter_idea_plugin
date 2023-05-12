package shop.itbug.fluttercheckversionx.actions.blog

import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.blog.showWriteBlogDialog


///发布博客的操作
class WriteConfigAction : MyAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.showWriteBlogDialog(e)
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        super.update(e)
    }
}