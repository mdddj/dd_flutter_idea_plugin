package shop.itbug.fluttercheckversionx.services

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import shop.itbug.fluttercheckversionx.constance.discordUrl
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PluginActions.*
import shop.itbug.fluttercheckversionx.util.RunUtil
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JComponent


enum class PluginActions(val title: String) {
    SearchPlugin(PluginBundle.get("search.pub.plugin")),
    RunBuilder(PluginBundle.get("run.build_runner.build")),
    FlutterClan(PluginBundle.get("flutter.clean")),
    FlutterPushPlugin(PluginBundle.get("dart.pub.publish")),
    JsonToFreezed("Json to Freezed"),
    Discord("Discord")
}

///用户面板
class MyUserBarFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "user-account"
    }

    override fun getDisplayName(): String {
        return "FlutterX"
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return MyUserAccountBar(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}


/**
 * 底部工具栏中的扩展操作
 *
 */
class MyUserAccountBar(var project: Project) : CustomStatusBarWidget {

    val icon = MyIcons.dartPluginIcon
    private val iconLabel = JBLabel(icon)


    override fun dispose() {
    }

    override fun ID(): String {
        return "dart plugin actions"
    }

    override fun install(statusBar: StatusBar) {
    }

    ///
    override fun getComponent(): JComponent {
        iconLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                e?.let {
                    showPop()
                }
                super.mouseClicked(e)
            }
        })
        iconLabel.text = getSdkVersion() ?: ""
        return iconLabel
    }


    //获取当前安装的flutter版本
    private fun getSdkVersion(): String? {
        return null
    }

    fun showPop() {
        val pop = createPop()
        val h = pop.content.preferredSize.height
        val w = pop.content.preferredSize.width
        pop.show(
            RelativePoint(
                Point(
                    iconLabel.locationOnScreen.x - w + iconLabel.preferredSize.width,
                    iconLabel.locationOnScreen.y - h
                )
            )
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createPop(): JBPopup {
        return JBPopupFactory.getInstance().createPopupChooserBuilder(PluginActions.entries.toList())
            .setItemChosenCallback { doActions(it) }
            .setRenderer { _, value, _, _, _ ->
                return@setRenderer JBLabel(value.title).apply {
                    border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
                }
            }
            .setTitle(PluginBundle.get("br.title"))
            .createPopup()
    }


    /**
     * 扩展操作
     * @param action 选择的操作
     */
    private fun doActions(action: PluginActions) {
        when (action) {
            SearchPlugin -> {
                SearchDialog(project).show()
            }

            RunBuilder -> runCommand("flutter pub run build_runner build")
            FlutterClan -> runCommand("flutter clean")
            FlutterPushPlugin -> runCommand(" dart pub publish")
            JsonToFreezed -> jsonToFreezedRun()
            Discord -> BrowserUtil.browse(discordUrl)
        }
    }


    private fun jsonToFreezedRun() {
        try {
            JsonToFreezedInputDialog(project).show()
        } catch (e: Exception) {
            println("...$e")
        }
    }

    private fun runCommand(code: String) {
        RunUtil.runCommand(project, "FlutterX", code)
    }

}