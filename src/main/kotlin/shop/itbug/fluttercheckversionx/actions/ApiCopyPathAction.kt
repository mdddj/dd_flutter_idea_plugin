package shop.itbug.fluttercheckversionx.actions

import cn.hutool.core.util.URLUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.util.toast


///复制路径
class ApiCopyPathAction: MyAction({"Copy Path"}) {
    override fun actionPerformed(e: AnActionEvent) {
        val url = e.api()!!.url
        val path = URLUtil.getPath(url)

        path.copyTextToClipboard().apply {
            e.project?.toast("Copy succeeded!")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.api()!=null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}