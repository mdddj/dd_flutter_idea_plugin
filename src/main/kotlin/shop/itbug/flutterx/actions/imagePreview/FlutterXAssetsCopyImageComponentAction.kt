package shop.itbug.flutterx.actions.imagePreview

import com.intellij.ide.actions.CopyPathProvider.Companion.QUALIFIED_NAME
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.flutterx.document.copyTextToClipboard

// 拷贝 Image.asset
class FlutterXAssetsCopyImageComponentAction : FlutterXAssetsVFAction() {
    override fun handleAction(project: Project, file: VirtualFile, e: AnActionEvent) {
        createCopyText(project, file)?.copyTextToClipboard()

    }


    override fun update(e: AnActionEvent) {
        super.update(e)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.getData(CommonDataKeys.PROJECT)
        if (file != null && project != null) {
            e.presentation.putClientProperty(QUALIFIED_NAME, createCopyText(project, file))
        }
    }

    private fun createCopyText(project: Project, file: VirtualFile): String? {
        getAssetRelativePath(project, file)?.let {
            val imageString = """
                Image.asset('$it')
            """.trimIndent()
            return imageString
        }
        return null
    }
}