package shop.itbug.fluttercheckversionx.actions.imagePreview

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard

class FlutterXAssetsSvgCopyAction : FlutterXAssetsVFAction() {
    override fun handleAction(project: Project, file: VirtualFile, e: AnActionEvent) {
        val string = """
              SvgPicture.asset('${getAssetRelativePath(project, file)}')
        """.trimIndent()
        string.copyTextToClipboard()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = file?.extension == "svg"
    }
}