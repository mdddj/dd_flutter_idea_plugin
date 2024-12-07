package shop.itbug.fluttercheckversionx.notif

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.PubCacheSizeCalcService
import shop.itbug.fluttercheckversionx.services.PubCacheSizeCalcService.Companion.TOPIC
import shop.itbug.fluttercheckversionx.services.noused.DartNoUsedCheckService
import shop.itbug.fluttercheckversionx.setting.IgPluginPubspecConfigList
import shop.itbug.fluttercheckversionx.tools.MyToolWindowTools
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.getPubspecYAMLFile
import shop.itbug.fluttercheckversionx.widget.DartPackageTable
import java.util.concurrent.Callable
import java.util.function.Function
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

class PubPluginVersionCheckNotification : EditorNotificationProvider, DumbAware {
    private var pubFile: YAMLFile? = null
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {

        return Function<FileEditor, JComponent?> {
            if (it.component.parent == null) return@Function null
            val panel = YamlFileNotificationPanel(it, project)
            if (pubFile != null && file.name == "pubspec.yaml") {
                return@Function panel
            }
            pubFile =
                ApplicationManager.getApplication().executeOnPooledThread(Callable { project.getPubspecYAMLFile() })
                    .get()
            if (it.component.parent != null && pubFile != null) {
                EditorNotifications.getInstance(project).updateNotifications(pubFile!!.virtualFile)
            }

            return@Function null
        }
    }

}


class YamlFileNotificationPanel(fileEditor: FileEditor, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtil.getEditorPaneBackground()) {


    private var checkLabel: HyperlinkLabel = createActionLabel(PluginBundle.get("check.flutter.plugin")) {
        DartPackageTable(project).show()
    }

    private val pubCacheSizeComponent = MyCheckPubCacheSizeComponent(project)

    init {

        myLinksPanel.add(pubCacheSizeComponent)

        icon(MyIcons.dartPluginIcon)
        text(PluginBundle.get("w.t"))

        myLinksPanel.add(checkLabel)

        val searchPluginLabel = createActionLabel(PluginBundle.get("search.pub.plugin")) {
            search()
        }
        myLinksPanel.add(searchPluginLabel)


        ///重新索引
        val reIndexLabel = createActionLabel(PluginBundle.get("pubspec_yaml_file_re_index")) {
            DartPackageCheckService.getInstance(project).startResetIndex()
            MyFileUtil.reIndexPubspecFile(project)
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
private class MyCheckPubCacheSizeComponent(val project: Project) : JBLabel(), PubCacheSizeCalcService.Listener,
    Disposable {
    init {
        project.messageBus.connect(PubCacheSizeCalcService.getInstance(project)).subscribe(TOPIC, this)
        Disposer.register(PubCacheSizeCalcService.getInstance(project), this)
        SwingUtilities.invokeLater {
            setDefaultText()
        }
        ToolTipManager.sharedInstance().registerComponent(this)
        toolTipText = PubCacheSizeCalcService.getInstance(project).getPubCacheDirPathString()
    }


    private fun setDefaultText() {
        text = "Pub Cache Size: " + PubCacheSizeCalcService.getInstance(project).getCurrentSizeFormatString()
    }

    override fun calcComplete(len: Long, formatString: String) {
        text = "Pub Cache Size: $formatString"
    }

    override fun dispose() {
        ToolTipManager.sharedInstance().unregisterComponent(this)
    }
}