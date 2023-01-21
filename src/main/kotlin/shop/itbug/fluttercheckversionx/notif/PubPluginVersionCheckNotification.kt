package shop.itbug.fluttercheckversionx.notif

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.lang.dart.DartFileType
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.getPubspecYAMLFile
import shop.itbug.fluttercheckversionx.window.AllPluginsCheckVersion
import java.util.function.Function
import javax.swing.JComponent

///pubyaml 窗口工具
class PubPluginVersionCheckNotification : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function<FileEditor, JComponent?> {
            project.getPubspecYAMLFile() ?: return@Function null
            if(file.fileType is DartFileType){
                return@Function null
            }
            YamlFileNotificationPanel(it, project)
        }
    }
}

class YamlFileNotificationPanel(private val fileEditor: FileEditor, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtil.getEditorPaneBackground()) {

    private var checkLabel: HyperlinkLabel

    init {
        icon(MyIcons.dartPluginIcon)
        text("梁典典的扩展工具")
        checkLabel = createActionLabel("检测新版本") {
            checkNewVersions()
        }
        myLinksPanel.add(checkLabel)

        val searchPluginLabel = createActionLabel("搜索插件") {
            search()
        }
        myLinksPanel.add(searchPluginLabel)
    }

    private fun checkNewVersions() {
        val file = project.getPubspecYAMLFile()
        file?.containingFile?.let { DaemonCodeAnalyzer.getInstance(project).restart(it) }
        val component = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(AllPluginsCheckVersion(project), fileEditor.component).createPopup()
        component.show(RelativePoint(checkLabel.locationOnScreen))

    }

    private fun search() {
        SearchDialog(project).show()
    }

    private fun openSetting() {

    }
}