package shop.itbug.fluttercheckversionx.actions.imagePreview

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard

class FlutterXCopyAssetPathAction : FlutterXAssetsVFAction() {
    override fun handleAction(project: Project, file: VirtualFile, e: AnActionEvent) {
        getAssetRelativePath(project, file)?.copyTextToClipboard()
    }
}