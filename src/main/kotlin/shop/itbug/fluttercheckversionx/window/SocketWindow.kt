package shop.itbug.fluttercheckversionx.window

import com.intellij.execution.ui.RunContentManagerImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import shop.itbug.fluttercheckversionx.form.socket.SocketRequestForm
import shop.itbug.fluttercheckversionx.hive.HiveWidget
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.window.android.FlutterXAndroidMigrateWindow
import shop.itbug.fluttercheckversionx.window.l10n.L10nWindow
import shop.itbug.fluttercheckversionx.window.logger.LoggerWindow
import shop.itbug.fluttercheckversionx.window.preview.ImagesPreviewWindow
import shop.itbug.fluttercheckversionx.window.privacy.PrivacyScanWindow
import shop.itbug.fluttercheckversionx.window.sp.SpWindow

/**
 * dio 的请求监听窗口, 在这个窗口中,会将手机里面的一系列请求在这个窗口中显示,并可以查看详细信息
 * 梁典典: 2022年04月29日11:12:51
 */
class FlutterXSocketWindow : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.title = "FlutterX"
        val instance = ContentFactory.getInstance()
        val socketRequestForm = SocketRequestForm(project, toolWindow)
        val createContent = instance.createContent(socketRequestForm, PluginBundle.get("window.idea.dio.title"), false)
        createContent.setDisposer(socketRequestForm) //销毁监听
        toolWindow.contentManager.addContent(createContent)

        val port = PluginStateService.appSetting.serverPort.toInt() // dio的监听端口
        if (AppService.getInstance().dioIsStart.not()) {
            toolWindow.activate {
                try {
                    DioApiService.getInstance().builder(port).start()
                    toolWindow.setIcon(RunContentManagerImpl.getLiveIndicator(MyIcons.flutter))
                    AppService.getInstance().setDioSocketState(true)
                } catch (_: Exception) {
                    project.toastWithError("Flutter dio listening service failed to start. Please try changing the port and restarting")
                }
            }
        } else {
            toolWindow.setIcon(RunContentManagerImpl.getLiveIndicator(MyIcons.flutter))
        }

        // sp工具
        val spWindow = SpWindow(project, toolWindow)
        val spContent = instance.createContent(spWindow, "Shared Preferences ${PluginBundle.get("tool")}", false)
        toolWindow.contentManager.addContent(spContent)
        spContent.setDisposer(spWindow)//销毁监听


        //hive 工具 开发中
        val hiveWindow = HiveWidget(project, toolWindow)
        val hiveContent = instance.createContent(hiveWindow, "Hive ${PluginBundle.get("tool")}", false)
        hiveContent.icon = AllIcons.General.Beta
        toolWindow.contentManager.addContent(hiveContent)
        hiveContent.setDisposer(hiveWindow)


        //logo 窗口
        val logWindow = LoggerWindow(project)

        val logContent = instance.createContent(logWindow, "Log", false)
        toolWindow.contentManager.addContent(logContent)
        logContent.setDisposer(logWindow)


        //隐私扫描工具窗口
        val privacyPanel = PrivacyScanWindow(project)
        val privacyContent = instance.createContent(
            privacyPanel, "IOS ${PluginBundle.get("are_you_ok_betch_insert_privacy_file_window_title")}", false
        )
        privacyContent.setDisposer(privacyPanel)
        toolWindow.contentManager.addContent(privacyContent)


        //android gradle 适配窗口
        val androidMigrateWindow = FlutterXAndroidMigrateWindow(project)
        val androidMigrateWindowContent = instance.createContent(
            androidMigrateWindow,
            "Android Gradle Migrate Tool",
            false
        )
        toolWindow.contentManager.addContent(androidMigrateWindowContent)


        // 资产图片拷贝窗口
        val imagesPreviewWindow = ImagesPreviewWindow(project, toolWindow)
        val imagesPreviewContent = instance.createContent(imagesPreviewWindow, "Assets Preview", true)
        toolWindow.contentManager.addContent(imagesPreviewContent)
        imagesPreviewContent.setDisposer(imagesPreviewWindow)


        // l10n多语言窗口
        val l10nWindow = L10nWindow(project, toolWindow)
        val l10nWindowContent = instance.createContent(l10nWindow, "l10n", false)
        toolWindow.contentManager.addContent(l10nWindowContent)
        l10nWindowContent.setDisposer(l10nWindow)

    }


}
