package shop.itbug.flutterx.actions.imagePreview

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.tools.log
import shop.itbug.flutterx.util.MyFileUtil

// QUALIFIED_NAME
abstract class FlutterXAssetsVFAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.getData(CommonDataKeys.PROJECT)
        if (file != null && project != null) {
            handleAction(project, file, e)
        } else {
            log().warn("Failed to get project file:  $file")
        }
    }

    fun getAssetRelativePath(project: Project, file: VirtualFile): String? {
        val root = MyFileUtil.getRootVirtualFile(file, project)
        if (root != null) {
            return VfsUtil.getRelativePath(file, root)
        }
        return null
    }


    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = file != null && project != null
        if (showFlutterXIcon) {
            e.presentation.icon = MyIcons.flutter
        }
        e.presentation.putClientProperty(ActionUtil.SUPPRESS_SUBMENU, true)

        super.update(e)
    }

    abstract fun handleAction(project: Project, file: VirtualFile, e: AnActionEvent)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    open val showFlutterXIcon: Boolean get() = true
}