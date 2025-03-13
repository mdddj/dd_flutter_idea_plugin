package shop.itbug.fluttercheckversionx.actions.imagePreview.toolbar

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.constance.Links
import shop.itbug.fluttercheckversionx.window.preview.ImagesPreviewWindow

class FlutterXAssetsPanelRefreshAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val comp = e.getData(ImagesPreviewWindow.KEY)
        comp?.refreshItems()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.General.Refresh
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}


class FlutterXAssetsPanelWindowOpenDocAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(Links.assetsPreviewDoc)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = AllIcons.Actions.Help
    }
}