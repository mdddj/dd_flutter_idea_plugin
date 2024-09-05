package shop.itbug.fluttercheckversionx.notif

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.DartPluginsPanel
import shop.itbug.fluttercheckversionx.setting.IgPluginPubspecConfigList
import shop.itbug.fluttercheckversionx.tools.MyToolWindowTools
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import shop.itbug.fluttercheckversionx.util.getPubspecYAMLFile
import shop.itbug.fluttercheckversionx.window.AllPluginsCheckVersion
import java.util.function.Function
import javax.swing.JComponent

class PubPluginVersionCheckNotification : EditorNotificationProvider, DumbAware {
    var pubFile: YAMLFile? = null
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {

        return Function<FileEditor, JComponent?> {
            if (pubFile != null && file.name == "pubspec.yaml") {
                return@Function YamlFileNotificationPanel(it, project)
            }
            val checkTask = object : Task.Backgroundable(project, "FlutterX:" + PluginBundle.get("init_editor_tool")) {
                override fun run(indicator: ProgressIndicator) {
                    pubFile = project.getPubspecYAMLFile()
                    ApplicationManager.getApplication().invokeLater {
                        EditorNotifications.getInstance(project)
                            .updateNotifications(this@PubPluginVersionCheckNotification)
                    }
                }

            }
            checkTask.queue()
            return@Function null
        }
    }

}

class YamlFileNotificationPanel(private val fileEditor: FileEditor, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtil.getEditorPaneBackground()) {

    private var checkLabel: HyperlinkLabel = createActionLabel(PluginBundle.get("check.flutter.plugin")) {
//        checkNewVersions()
        DartPluginsPanel.showInCenter(project)
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
            DartPackageCheckService.getInstance(project).resetIndex()
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
            IgPluginPubspecConfigList.showInPopup(project)
        }
        myLinksPanel.add(igPackageLabel)
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
        val component = JBPopupFactory.getInstance().createComponentPopupBuilder(AllPluginsCheckVersion(project) {
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