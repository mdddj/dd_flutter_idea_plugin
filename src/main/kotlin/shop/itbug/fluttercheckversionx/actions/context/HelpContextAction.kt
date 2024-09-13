package shop.itbug.fluttercheckversionx.actions.context

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction


enum class SiteDocument(val url: String) {
    Dio("https://flutterx.itbug.shop/starter.html"),
    Hive("https://flutterx.itbug.shop/hive%E7%BC%93%E5%AD%98%E5%B7%A5%E5%85%B7.html"),
    Sp("https://flutterx.itbug.shop/shared-preferences.html")
}

///文档按钮
class HelpContextAction(val document: SiteDocument) :
    DumbAwareAction("Document", "View documentation", AllIcons.Actions.Help) {
    override fun actionPerformed(p0: AnActionEvent) {
        BrowserUtil.browse(document.url)
    }

}