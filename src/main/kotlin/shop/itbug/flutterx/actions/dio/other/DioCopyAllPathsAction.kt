package shop.itbug.flutterx.actions.dio.other

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.config.DoxListeningSetting
import shop.itbug.flutterx.document.copyTextToClipboard
import shop.itbug.flutterx.dsl.formatUrl
import shop.itbug.flutterx.socket.Request
import shop.itbug.flutterx.socket.service.AppService

/**
 *
 */

///拷贝列表里面的全部paths
class DioCopyAllPathsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val text = getApiList().map {
            it.formatUrl(
                DoxListeningSetting(
                    showHost = false, showQueryParams = false
                )
            )
        }.distinct().joinToString(separator = "\n")
        text.copyTextToClipboard()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun getApiList(): List<Request> {
        return AppService.getInstance().getCurrentProjectAllRequest()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getApiList().isNotEmpty()
        e.presentation.icon = AllIcons.Actions.Copy
        super.update(e)
    }
}