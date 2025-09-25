package shop.itbug.fluttercheckversionx.notif

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.common.yaml.PubspecYamlFileTools
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PubCacheSizeCalcService
import shop.itbug.fluttercheckversionx.services.PubCacheSizeCalcService.Companion.TOPIC
import shop.itbug.fluttercheckversionx.services.noused.DartNoUsedCheckService
import shop.itbug.fluttercheckversionx.setting.IgPluginPubspecConfigList
import shop.itbug.fluttercheckversionx.tools.MyToolWindowTools
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import java.awt.event.InputEvent
import java.util.function.Function
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

class PubPluginVersionCheckNotification : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function<FileEditor, JComponent?> {
            if (project.isDisposed) return@Function null
            if (it.component.parent == null) return@Function null
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@Function null
            if (file.name == "pubspec.yaml" && psiFile is YAMLFile) {

                val isFlutterProject =
                    runBlocking(Dispatchers.IO) { PubspecYamlFileTools.create(psiFile).isFlutterProject() }

                if (!isFlutterProject) return@Function null
                val panel = YamlFileNotificationPanel(it, psiFile, project)
                return@Function panel
            }
            return@Function null
        }
    }

}


private class YamlFileNotificationPanel(fileEditor: FileEditor, val file: YAMLFile, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtil.getEditorPaneBackground()) {

    private val pubCacheSizeComponent = MyCheckPubCacheSizeComponent(project)

    init {
        Disposer.register(PubCacheSizeCalcService.getInstance(project), pubCacheSizeComponent)

        myLinksPanel.add(pubCacheSizeComponent)

        icon(MyIcons.dartPluginIcon)
        text(PluginBundle.get("w.t"))

        val searchPluginLabel = createActionLabel(PluginBundle.get("search.pub.plugin")) {
            search()
        }
        myLinksPanel.add(searchPluginLabel)


        ///重新索引
        val reIndexLabel = createActionLabel(PluginBundle.get("pubspec_yaml_file_re_index")) {
            MyFileUtil.reIndexWithVirtualFile(file.virtualFile)
            DaemonCodeAnalyzer.getInstance(project).restart(file)
        }

        myLinksPanel.add(reIndexLabel)


        ///打开隐私扫描窗口
        val openPrivacyWindowLabel =
            createActionLabel(PluginBundle.get("are_you_ok_betch_insert_privacy_file_window_title")) {
                doOpenPrivacyWindow()
            }

        myLinksPanel.add(openPrivacyWindowLabel)


        ///管理忽略的包
        val igPackageLabel = createActionLabel("Ignore packages") {
            IgPluginPubspecConfigList.showInPopup(project, file)
        }
        myLinksPanel.add(igPackageLabel)

        ///检查没有被使用的包
        val noUsedCheck = createActionLabel(PluginBundle.get("check_un_used_package")) {
            DartNoUsedCheckService.getInstance(project).checkUnUsedPackaged()
        }
        myLinksPanel.add(noUsedCheck)
    }

    ///打开隐私扫描工具窗口
    private fun doOpenPrivacyWindow() {
        val myToolWindow = MyToolWindowTools.getMyToolWindow(project)
        myToolWindow?.let {
            it.activate {
                val content = it.contentManager.getContent(4)
                if (content != null) {
                    it.contentManager.setSelectedContent(content)
                }
            }
        }
    }

    private fun search() {
        SearchDialog(project).show()
    }

}


///计算pub cache 占用大小
private class MyCheckPubCacheSizeComponent(project: Project) : HyperlinkLabel(), PubCacheSizeCalcService.Listener,
    Disposable, BulkFileListener {
    val cacheService = PubCacheSizeCalcService.getInstance(project)
    private var isDisposed = false

    init {
        project.messageBus.connect(cacheService).subscribe(TOPIC, this)
        project.messageBus.connect(parentDisposable = this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            this
        )
        Disposer.register(cacheService, this)
        SwingUtilities.invokeLater {
            setDefaultText()
        }
        ToolTipManager.sharedInstance().registerComponent(this)
        toolTipText = cacheService.getPubCacheDirPathString()
        ApplicationManager.getApplication().invokeLater {
            cacheService.refreshCheck()
        }
    }

    override fun fireHyperlinkEvent(inputEvent: InputEvent?) {
        cacheService.openDir()
        super.fireHyperlinkEvent(inputEvent)
    }


    private fun setDefaultText() {
        setHyperlinkText("Pub Cache Size: " + cacheService.getCurrentSizeFormatString())
    }

    override fun calcComplete(len: Long, formatString: String) {
        SwingUtilities.invokeLater {
            if (!isDisposed) {
                setHyperlinkText("Pub Cache Size: $formatString")
            }

        }

    }

    override fun dispose() {
        println("dispose pub cache size widget")
        isDisposed = true
        ToolTipManager.sharedInstance().unregisterComponent(this)
    }

    override fun after(events: List<VFileEvent>) {
        val cachePath = cacheService.getCachePath()
        if (events.isNotEmpty() && cachePath.isNullOrBlank()
                .not() && events.any { it.file?.path?.startsWith(cachePath) == true }
        ) {
            cacheService.refreshCheck()
        }
        super.after(events)
    }
}

