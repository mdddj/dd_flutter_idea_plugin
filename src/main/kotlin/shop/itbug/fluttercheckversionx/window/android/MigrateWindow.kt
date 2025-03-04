package shop.itbug.fluttercheckversionx.window.android

import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestFactory
import com.intellij.diff.DiffRequestPanel
import com.intellij.diff.tools.util.DiffDataKeys
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.wm.ToolWindow
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.manager.FlutterAndroidMigrateManager
import java.awt.Dimension
import java.io.File
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener


sealed class AndroidMigrateFile(open val file: VirtualFile) {
    var newFile: VirtualFile? = null
    fun findRelativePath(project: Project): String {
        if (project.isDisposed) return file.path
        val projectBasePath = project.guessProjectDir() ?: return file.path
        return VfsUtilCore.findRelativePath(
            projectBasePath, file, File.separatorChar
        ) ?: file.path
    }

    fun doReplace(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            updatePsi(project, file)
        }
    }

    fun createBackFile(): VirtualFile {
        newFile = runReadAction {
            LightVirtualFile(
                file.name, file.fileType, file.readText()
            )
        }
        return newFile!!
    }

    abstract fun updatePsi(project: Project, file: VirtualFile)
}

class AndroidBuildFile(val androidBuildFile: VirtualFile) : AndroidMigrateFile(androidBuildFile) {

    override fun updatePsi(project: Project, vf: VirtualFile) {
        FlutterAndroidMigrateManager.getInstance(project).getNewAndroidBuildFile(vf)
    }


}

class AndroidAppBuildFile(val androidAppBuildFile: VirtualFile) : AndroidMigrateFile(androidAppBuildFile) {

    override fun updatePsi(project: Project, vf: VirtualFile) {
        FlutterAndroidMigrateManager.getInstance(project).getNewAppBuildFile(vf)
    }
}

class AndroidSettingsFile(override val file: VirtualFile) : AndroidMigrateFile(file) {

    override fun updatePsi(project: Project, file: VirtualFile) {
        FlutterAndroidMigrateManager.getInstance(project).getNewSettingsGradleFile(file)
    }

}

/**
 * android 适配窗口
 */
class FlutterXAndroidMigrateWindow(val project: Project, val toolWindow: ToolWindow) : BorderLayoutPanel(),
    ListSelectionListener {

    private val androidService = FlutterAndroidMigrateManager.getInstance(project)
    private val sp = OnePixelSplitter()

    //将要修改的文件列表
    private val willUpdateFiles = mutableSetOf<AndroidMigrateFile>()
    private var diffPanel: DiffRequestPanel? = null
    private val list = JBList<AndroidMigrateFile>().apply {
        model = DefaultListModel()
        cellRenderer = FileListRender(project)
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        addListSelectionListener(this@FlutterXAndroidMigrateWindow)
        preferredSize = Dimension(240, preferredSize.height)

    }


    init {
        ApplicationManager.getApplication().invokeAndWait({
            tryGetFiles()
        }, ModalityState.defaultModalityState())
        sp.splitterProportionKey = "FlutterXAndroidMigrateWindow"
        sp.firstComponent = JBScrollPane(list).apply {
            preferredSize = Dimension(240, list.height)
            border = BorderFactory.createEmptyBorder()
        }
        addToCenter(sp)
    }


    //获取相关文件
    private fun tryGetFiles() {
        val findAndroidBuildFile = androidService.findAndroidBuildFile()
        if (findAndroidBuildFile != null) {
            val file = AndroidBuildFile(findAndroidBuildFile)
            willUpdateFiles.add(file)
            getListModel().addElement(file)
            showDiffWindow(file)
        }
        val findAppBuildFile = androidService.findAppBuildFile()
        if (findAppBuildFile != null) {
            val file = AndroidAppBuildFile(findAppBuildFile)
            willUpdateFiles.add(file)
            getListModel().addElement(file)
        }

        val settingsFile = androidService.findSettingsFile()
        if (settingsFile != null) {
            val file = AndroidSettingsFile(settingsFile)
            willUpdateFiles.add(file)
            getListModel().addElement(file)
        }
        list.selectedIndex = 0

    }


    private fun getListModel(): DefaultListModel<AndroidMigrateFile> {
        return list.model as DefaultListModel<AndroidMigrateFile>
    }


    ///显示 diff 窗口
    private fun showDiffWindow(file: AndroidMigrateFile) {
        val newFile = file.createBackFile()
        val request =
            DiffRequestFactory.getInstance().createFromFiles(project, file.file, newFile)
        val panel = DiffManager.getInstance().createRequestPanel(project, androidService, null)
        request.putUserData(DiffUserDataKeysEx.CONTEXT_ACTIONS, createActionsList())
        request.putUserData(FlutterAndroidMigrateManager.FILE, file)
        file.updatePsi(project, newFile)

        panel.setRequest(request)
        if (diffPanel == null) {
            diffPanel = panel
            sp.secondComponent = panel.component
        } else {
            diffPanel!!.setRequest(request)
        }
    }

    private fun createActionsList(): List<AnAction> {
        return listOf(FlutterAndroidMigrateAction())
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting) {
            val index = e.firstIndex
            if (index >= 0) {
                val select = list.selectedValue
                showDiffWindow(select)
            }
        }
    }
}


class FileListRender(val project: Project) : ColoredListCellRenderer<AndroidMigrateFile>() {
    override fun customizeCellRenderer(
        p0: JList<out AndroidMigrateFile?>, p1: AndroidMigrateFile?, p2: Int, p3: Boolean, p4: Boolean
    ) {
        p1?.let {
            icon = it.file.fileType.icon
            append(it.findRelativePath(project))
        }

    }
}


//将更改应用到项目中
class FlutterAndroidMigrateAction : AnAction("同意这些更改", "", AllIcons.Actions.Checked) {


    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(DiffDataKeys.DIFF_REQUEST) ?: return
        val file = editor.getUserData(FlutterAndroidMigrateManager.FILE)!!
        LocalFileSystem.getInstance().apply {
            file.doReplace(e.project!!)
        }

    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(DiffDataKeys.DIFF_REQUEST) ?: return
        val file = editor.getUserData(FlutterAndroidMigrateManager.FILE)
        with(e.presentation) {
            isVisible = file != null && e.project != null
        }
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}