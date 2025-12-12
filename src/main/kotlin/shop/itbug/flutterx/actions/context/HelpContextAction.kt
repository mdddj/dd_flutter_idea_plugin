package shop.itbug.flutterx.actions.context

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.util.Key
import javax.swing.JComponent


enum class SiteDocument(val url: String) {
    Dio("https://mdddj.github.io/flutterx-doc/en/dio/starter"),
    Hive("https://mdddj.github.io/flutterx-doc/en/hive/hive-cache-tool/"),
    Sp("https://mdddj.github.io/flutterx-doc/en/shared_p/shared-preferences/"),
    AssetsPreview("https://mdddj.github.io/flutterx-doc/en/assets/asset-preview-window/"),
    L10n("https://mdddj.github.io/flutterx-doc/en/other/l10n-editor/"),
    Log("https://mdddj.github.io/flutterx-doc/en/other/log-tools/");

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
    }

}