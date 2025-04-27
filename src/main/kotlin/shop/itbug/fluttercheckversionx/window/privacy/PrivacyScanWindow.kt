package shop.itbug.fluttercheckversionx.window.privacy

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ColoredText
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

private const val defaultPrivacyFileText = """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>NSPrivacyTrackingDomains</key>
	<array/>
	<key>NSPrivacyAccessedAPITypes</key>
	<array/>
	<key>NSPrivacyCollectedDataTypes</key>
	<array/>
	<key>NSPrivacyTracking</key>
	<false/>
</dict>
</plist>
"""

private const val privacyFileName = "PrivacyInfo.xcprivacy"

///iOS隐私扫描工具


class PrivacyScanWindow(val project: Project) : BorderLayoutPanel(), Disposable, ActionListener {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tipLabel = JBLabel(" " + PluginBundle.get("privacy_info_tips"))

    private val pathList = JBList(ListModel()).apply {
        cellRenderer = ListRender()
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index: Int = this@apply.locationToIndex(e.point)
                if (index >= 0) {
                    openOnEditor(index)
                }
            }
        })
    }

    private val button = JButton(PluginBundle.get("scan_privacy"))
    private val button2 = JButton(PluginBundle.get("are_you_ok_betch_insert_privacy_file_button_title")).apply {
        addActionListener {
            batchInsert()
        }
    }
    private val toolbar = JToolBar().apply {
        isFloatable = false
        add(button)
        add(button2)
        add(tipLabel)
    }

    init {
        addToTop(toolbar)
        addToCenter(pathList.scroll())
        button.addActionListener(this)
        SwingUtilities.invokeLater {
            scanPackages()
        }
    }

    /**
     * 扫描第三方依赖包
     */
    private fun scanPackages() {
        val files = ProjectRootManager.getInstance(project).orderEntries().librariesOnly().roots(OrderRootType.CLASSES)
        coroutineScope.launch {
            files.roots.map { coroutineScope.async(Dispatchers.IO) { addPackageModel(it) } }.awaitAll()
        }
    }

    private fun addPackageModel(file: VirtualFile) {
        if (file.isDirectory) {
            val packageRoot = FlutterPackageRoot(file)
            val iosDir = packageRoot.getIosDirectory()
            packageRoot.findPrivacyFile()
            if (iosDir != null) {
                getListModel().addElement(packageRoot)
            }
        }
    }


    private inner class ListModel() : DefaultListModel<FlutterPackageRoot>() {

        fun getAllFiles(): List<FlutterPackageRoot> {
            return this.elements().toList()
        }
    }

    private inner class FlutterPackageRoot(val file: VirtualFile) {
        var privacyFile: VirtualFile? = null
        var iosFolder: VirtualFile? = null

        //获取 ios目录
        fun getIosDirectory(): VirtualFile? {
            iosFolder = file.readChild("ios")
            return iosFolder
        }


        //在 ios目录下查找隐私文件
        private fun findPrivacyFileInIosFolder(): VirtualFile? {
            return getIosDirectory()?.readChild(privacyFileName)
        }

        //在 resource目录下查找隐私文件
        private fun findPrivacyFileInResource(): VirtualFile? {
            return getIosDirectory()?.readChild("Resources")?.readChild(privacyFileName)
        }

        // 查找隐私文件
        fun findPrivacyFile(): VirtualFile? {
            privacyFile = findPrivacyFileInIosFolder() ?: findPrivacyFileInResource()
            return privacyFile
        }

        //创建隐私文件
        suspend fun cratePrivacyFile() {
            iosFolder?.let {
                val iosPsiDirectory: PsiDirectory =
                    readAction { PsiManager.getInstance(project).findDirectory(it) } ?: return
                val privacyFile: PsiFile = readAction {
                    PsiFileFactory.getInstance(project).createFileFromText(
                        privacyFileName, PlainTextFileType.INSTANCE,
                        defaultPrivacyFileText
                    )
                }
                withContext(Dispatchers.EDT) {
                    WriteAction.compute<PsiElement, Throwable> {
                        iosPsiDirectory.add(privacyFile)
                    }
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(file.path))
                }

            }
        }

        private fun VirtualFile.readChild(name: String): VirtualFile? =
            ApplicationManager.getApplication().executeOnPooledThread<VirtualFile?> { this.findChild(name) }.get()


    }

    private inner class ListRender : ColoredListCellRenderer<FlutterPackageRoot>() {
        override fun customizeCellRenderer(
            p0: JList<out FlutterPackageRoot?>,
            p1: FlutterPackageRoot?,
            p2: Int,
            p3: Boolean,
            p4: Boolean
        ) {

            val vf = p1?.file ?: return
            vf.let {
                append(
                    it.name,
                    SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                )
                append("\t\t")
                val privacyFile = p1.privacyFile
                if (privacyFile != null) {
                    //有隐私政策文件
                    val attr = SimpleTextAttributes(
                        SimpleTextAttributes.STYLE_PLAIN,
                        JBUI.CurrentTheme.Link.Foreground.ENABLED
                    )
                    append(ColoredText.builder().append("✅Ok", attr).build())
                    append("\t\t\t")
                    append(
                        ColoredText.builder()
                            .append(PluginBundle.get("click_open_the_file"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
                            .build()
                    )
                } else {
                    //没有
                    append(
                        ColoredText.builder().append(
                            "❌${PluginBundle.get("not_found_the_privacy_file")}",
                            SimpleTextAttributes.ERROR_ATTRIBUTES
                        ).build()
                    )
                }
            }
        }

    }

    private fun openOnEditor(index: Int) {
        val file = (this.pathList.model as ListModel).elementAt(index)
        val pf = file.privacyFile
        pf?.let {
            FileEditorManager.getInstance(project).openFile(it)
        }
    }


    /**
     * 批量插件默认的
     */
    private fun batchInsert() {
        val result = Messages.showOkCancelDialog(
            project,
            PluginBundle.get("are_you_ok_betch_insert_privacy_file"),
            PluginBundle.get("are_you_ok_betch_insert_privacy_file_title"),
            PluginBundle.get("are_you_ok_betch_insert_privacy_file_ok"),
            PluginBundle.get("are_you_ok_betch_insert_privacy_file_cancel"),
            Messages.getQuestionIcon()
        )
        if (result == Messages.YES) {
            val dirs = (this@PrivacyScanWindow.getListModel()).getAllFiles()
            val notFoundPrivacyDirs = dirs.filter { it.privacyFile == null }
            coroutineScope.launch {
                notFoundPrivacyDirs.map { coroutineScope.async(Dispatchers.IO) { it.cratePrivacyFile() } }.awaitAll()
                refreshListModel()
            }

        }
    }

    private fun refreshListModel() {
        getListModel().clear()
        scanPackages()
    }


    override fun dispose() {
        button.removeActionListener(this)
        coroutineScope.cancel()
    }

    private fun getListModel() = pathList.model as ListModel
    override fun actionPerformed(e: ActionEvent?) {
        getListModel().clear()
        scanPackages()
    }
}
