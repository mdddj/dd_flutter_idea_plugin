package shop.itbug.fluttercheckversionx.actions.context

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.util.Key
import javax.swing.JComponent


enum class SiteDocument(val url: String) {
    Dio("https://flutterx.itbug.shop/starter.html"),
    Hive("https://flutterx.itbug.shop/hive%E7%BC%93%E5%AD%98%E5%B7%A5%E5%85%B7.html"),
    Sp("https://flutterx.itbug.shop/shared-preferences.html"),
    AssetsPreview("https://flutterx.itbug.shop/%E8%B5%84%E4%BA%A7%E9%A2%84%E8%A7%88%E7%AA%97%E5%8F%A3.html"),
    L10n("https://flutterx.itbug.shop/l10n-editor.html"),
    Log("https://flutterx.itbug.shop/log.html");

    fun open() {
        BrowserUtil.open(url)
    }
}

///文档按钮
class HelpContextAction() :
    DumbAwareAction("Document", "View documentation", AllIcons.Actions.Help) {
    override fun actionPerformed(p0: AnActionEvent) {
        p0.getSiteDocument()?.let {
            BrowserUtil.browse(it.url)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.getSiteDocument() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun AnActionEvent.getSiteDocument(): SiteDocument? {
        val comp = getData(PlatformDataKeys.CONTEXT_COMPONENT) ?: return null
        val doc = (comp as? JComponent)?.getUserData(DataKey)
        if (doc != null) return doc
        val comps = (comp as? JComponent)?.components ?: return null
        val find = comps.find { (it as? JComponent)?.getUserData(DataKey) != null }
        return (find as? JComponent)?.getUserData(DataKey)
    }

    companion object {
        val DataKey = Key.create<SiteDocument>("SiteDocument")
        val ACTION = ActionManager.getInstance().getAction("HelpAction") as HelpContextAction
    }

}