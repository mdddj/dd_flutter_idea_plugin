package shop.itbug.fluttercheckversionx.actions.imagePreview.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import shop.itbug.fluttercheckversionx.window.preview.ImagesPreviewWindow

class FlutterXAssetsPanelRefreshAction : DumbAwareAction(AllIcons.General.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
        e.getData(ImagesPreviewWindow.KEY)?.refreshItems()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(ImagesPreviewWindow.KEY) != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
