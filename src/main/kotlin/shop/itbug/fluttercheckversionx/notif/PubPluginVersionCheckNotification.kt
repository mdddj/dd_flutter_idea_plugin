package shop.itbug.fluttercheckversionx.notif

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
import icons.FlutterIcons
import io.flutter.utils.UIUtils
import shop.itbug.fluttercheckversionx.window.AllPluginsCheckVersion
import java.util.function.Function
import javax.swing.JComponent
import javax.swing.JSeparator
import javax.swing.SwingConstants

///pubyaml 窗口工具
class PubPluginVersionCheckNotification : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function<FileEditor, JComponent?> {
            YamlFileNotificationPanel(it, project)
        }
    }
}

class YamlFileNotificationPanel(val fileEditor: FileEditor, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtils.getEditorNotificationBackgroundColor().defaultColor) {

    var checkLabel: HyperlinkLabel

    init {
        icon(FlutterIcons.Flutter);
        text("梁典典的扩展工具")
        checkLabel = createActionLabel("检测新版本") {
            checkNewVersions()
        }

        myLinksPanel.add(checkLabel)

        val searchPluginLabel = createActionLabel("搜索插件") {
            search()
        }
        myLinksPanel.add(searchPluginLabel)


        //添加一个分割线
        myLinksPanel.add(JSeparator(SwingConstants.VERTICAL))


        val settingLabel = createActionLabel("设置") {
            openSetting()
        }

        myLinksPanel.add(settingLabel)
    }

    private fun checkNewVersions() {
        val component = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(AllPluginsCheckVersion(project), fileEditor.component).createPopup()
        component.show(RelativePoint(checkLabel.locationOnScreen))
    }

    private fun search() {

    }

    private fun openSetting() {

    }
}