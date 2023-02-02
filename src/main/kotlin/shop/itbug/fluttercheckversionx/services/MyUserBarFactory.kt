package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import org.jetbrains.plugins.terminal.TerminalView
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PluginActions.*
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JComponent


enum class PluginActions(val title: String) {
    SearchPlugin("搜索pub包"), CheckVersion("检测版本更新"), GetAllPlugins("获取全部插件"), RunBuilder("生成代码"), FlutterClan(
        "清理"
    ),
    FlutterPushPlugin("发布插件"),
    BuildApk("构建安卓包"),
    BuildIos("构建IOS包"),
    FlutterDoctor("颤抖医生"),
    JsonToFreezed("Json to Freezed")
}

///用户面板
class MyUserBarFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "user-account"
    }

    override fun getDisplayName(): String {
        return "典典账号登录"
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

    private fun createPop(): JBPopup {
        return JBPopupFactory.getInstance().createPopupChooserBuilder(values().asList())
            .setItemChosenCallback { doActions(it) }
            .setRenderer { _, value, _, _, _ ->
                return@setRenderer JBLabel(value.title).apply {
                    border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
                }
            }
            .setTitle("典典工具包")
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

            CheckVersion -> {
                val pubspecFile = MyPsiElementUtil.getPubSecpYamlFile(project)
                pubspecFile?.let {
                    val plugins = MyPsiElementUtil.getAllPlugins(project)
                    print(plugins)
                }
            }

            GetAllPlugins -> MyPsiElementUtil.getAllFlutters(project)
            RunBuilder -> runCommand("flutter pub run build_runner build")
            FlutterClan -> runCommand("flutter clean")
            FlutterPushPlugin -> runCommand(" dart pub publish")
            BuildApk -> runCommand("flutter build apk")
            BuildIos -> runCommand("flutter build ios")
            FlutterDoctor -> runCommand("flutter doctor")
            JsonToFreezed -> jsonToFreezedRun()
        }
    }


    private fun jsonToFreezedRun() {
        JsonToFreezedInputDialog(project).show()
    }

    private fun runCommand(code: String) {
        TerminalView.getInstance(project).createLocalShellWidget(project.basePath, "梁典典").executeCommand(code)
    }

}