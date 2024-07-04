package shop.itbug.fluttercheckversionx.notif

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.lang.dart.DartFileType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.tools.MyToolWindowTools
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import shop.itbug.fluttercheckversionx.util.getPubspecYAMLFile
import shop.itbug.fluttercheckversionx.window.AllPluginsCheckVersion
import java.util.function.Function
import javax.swing.JComponent

class PubPluginVersionCheckNotification : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function<FileEditor, JComponent?> {

            val pub = project.getPubspecYAMLFile()

            pub ?: return@Function null
            if (file.fileType is DartFileType) {
                return@Function null
            }
            val filename: String = file.name
            if (filename != "pubspec.yaml") {
                return@Function null
            }
            YamlFileNotificationPanel(it, project)
        }
    }
}

class YamlFileNotificationPanel(private val fileEditor: FileEditor, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtil.getEditorPaneBackground()) {

    private var checkLabel: HyperlinkLabel = createActionLabel(PluginBundle.get("check.flutter.plugin")) {
        checkNewVersions()
    }

    init {
        icon(MyIcons.dartPluginIcon)
        text(PluginBundle.get("w.t"))

        myLinksPanel.add(checkLabel)

        val searchPluginLabel = createActionLabel(PluginBundle.get("search.pub.plugin")) {
            search()
        }
        myLinksPanel.add(searchPluginLabel)


        ///重新索引
        val reIndexLabel = createActionLabel(PluginBundle.get("pubspec_yaml_file_re_index")) {
            doReIndex()
        }
        myLinksPanel.add(reIndexLabel)


        ///打开隐私扫描窗口
        val openPrivacyWindowLabel =
            createActionLabel(PluginBundle.get("are_you_ok_betch_insert_privacy_file_window_title")) {
                doOpenPrivacyWindow()
            }

        myLinksPanel.add(openPrivacyWindowLabel)

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

    ///重新索引
    @OptIn(DelicateCoroutinesApi::class)
    private fun doReIndex() {
        GlobalScope.launch {
            MyPsiElementUtil.getPubSpecYamlFile(project)?.let { _ ->
                run {
                    WriteCommandAction.runWriteCommandAction(project) {
                        VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                    }
                }
            }
        }

    }

    private fun checkNewVersions() {
        val file = project.getPubspecYAMLFile()
        file?.containingFile?.let { DaemonCodeAnalyzer.getInstance(project).restart(it) }
        val component = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(AllPluginsCheckVersion(project) {
                file?.containingFile?.let {
                    DaemonCodeAnalyzer.getInstance(project).restart(it)
                }
            }, fileEditor.component).createPopup()
        component.show(RelativePoint(checkLabel.locationOnScreen))

    }

    private fun search() {
        SearchDialog(project).show()
    }

}