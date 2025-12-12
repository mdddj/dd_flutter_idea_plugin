package shop.itbug.flutterx.actions.imagePreview

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FlutterXAssetsOpenFileAction : FlutterXAssetsVFAction() {
    override fun handleAction(project: Project, file: VirtualFile, e: AnActionEvent) {
        FileEditorManager.getInstance(project).openFile(file)
    }

    override val showFlutterXIcon: Boolean
        get() = false
}