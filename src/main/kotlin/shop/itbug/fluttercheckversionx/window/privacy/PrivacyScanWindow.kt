package shop.itbug.fluttercheckversionx.window.privacy

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ColoredText
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JToolBar


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
class PrivacyScanWindow(val project: Project) : BorderLayoutPanel() {


    private val tipLabel = JBLabel(" " + PluginBundle.get("privacy_info_tips"))

    private val pathList = JBList<VirtualFile>().apply {
        model = ListModel(emptyList())
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
        scanPackages()
        button.addActionListener {
            scanPackages()
        }
    }


    ///扫描第三方依赖包
    private fun scanPackages() {
        pathList.model = ListModel(emptyList())
        val list = mutableListOf<VirtualFile>()
        val files = ProjectRootManager.getInstance(project).orderEntries().librariesOnly().roots(OrderRootType.CLASSES)
        for (file in files.roots) {
            if (file.isDirectory) {
                ApplicationManager.getApplication().invokeLater {
                    val iosDir = file.findChild("ios")
                    if (iosDir != null) {
                        list.add(file)
                        pathList.model = ListModel(list)
                    }
                }
            }
        }
    }


    private inner class ListModel(val files: List<VirtualFile>) : DefaultListModel<VirtualFile>() {
        init {
            addAll(files)
        }
    }

    private inner class ListRender : ColoredListCellRenderer<VirtualFile>() {
        override fun customizeCellRenderer(
            p0: JList<out VirtualFile>,
            p1: VirtualFile?,
            p2: Int,
            p3: Boolean,
            p4: Boolean
        ) {
            p1?.let {
                append(
                    it.name,
                    SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                )
                append("\t\t")
                val privacyFile = it.findPrivacyFile()
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
        val pf = file.findPrivacyFile()
        pf?.let {
            FileEditorManager.getInstance(project).openFile(pf)
        }
    }


    /**
     * 批量插件默认的
     */
    private fun batchInsert() {
        ApplicationManager.getApplication().invokeLater {
            val result = Messages.showOkCancelDialog(
                project,
                PluginBundle.get("are_you_ok_betch_insert_privacy_file"),
                PluginBundle.get("are_you_ok_betch_insert_privacy_file_title"),
                PluginBundle.get("are_you_ok_betch_insert_privacy_file_ok"),
                PluginBundle.get("are_you_ok_betch_insert_privacy_file_cancel"),
                Messages.getQuestionIcon()
            )
            if (result == Messages.YES) {
                ApplicationManager.getApplication().invokeLater {
                    val dirs = (this.pathList.model as ListModel).files
                    val notFoundPrivacyDirs = dirs.filter { it.findPrivacyFile() == null }
                    val task = object : Task.Backgroundable(
                        project,
                        "${PluginBundle.get("are_you_ok_betch_insert_privacy_file_insert")}${privacyFileName}${
                            PluginBundle.get(
                                "are_you_ok_betch_insert_privacy_file_file"
                            )
                        }",
                        true
                    ) {
                        override fun run(pi: ProgressIndicator) {
                            var i = 0
                            notFoundPrivacyDirs.forEach {
                                pi.text = it.name
                                createPrivacyFile(project, it)
                                i++
                                pi.fraction = (i / notFoundPrivacyDirs.size).toDouble()

                            }
                        }
                    }
                    ProgressManager.getInstance().run(task)
                }
            }
        }
    }
}


private fun VirtualFile.findPrivacyFile(): VirtualFile? {
    val iosDir = this.findChild("ios")!!
    var privacyFile = iosDir.findChild(privacyFileName)
    if (privacyFile == null) {
        ///去resource/目录下面兆
        iosDir.findChild("Resources")?.let { resources ->
            privacyFile = resources.findChild(privacyFileName)
        }
    }
    return privacyFile
}

private fun createPrivacyFile(project: Project, baseDir: VirtualFile) {
    // 获取虚拟文件管理器


    val iosDir = baseDir.findChild("ios")!!
    lateinit var privacyFile: PsiFile
    runReadAction {
        privacyFile = PsiFileFactory.getInstance(project).createFileFromText(
            privacyFileName, PlainTextFileType.INSTANCE,
            defaultPrivacyFileText
        )
    }
    // 将文件写入磁盘
    WriteCommandAction.runWriteCommandAction(project) {
        try {
            // 将文件添加到文件系统
            val dir: PsiDirectory? = PsiManager.getInstance(project).findDirectory(iosDir)
            dir?.add(privacyFile)
            // 刷新文件系统
            iosDir.canonicalFile?.let { virtualFile ->
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(virtualFile.path))
            }
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // 通知 IDE 文件已更改

}