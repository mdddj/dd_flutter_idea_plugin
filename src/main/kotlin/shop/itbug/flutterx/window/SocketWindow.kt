package shop.itbug.flutterx.window

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.jewel.bridge.addComposeTab
import shop.itbug.flutterx.common.yaml.hasPubspecYamlFile
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.config.DoxListeningSetting
import shop.itbug.flutterx.dialog.FlutterDownloadPanel
import shop.itbug.flutterx.dialog.icons.CupertinoIconsDialog
import shop.itbug.flutterx.dialog.icons.MaterialIconsDialog
import shop.itbug.flutterx.form.socket.SocketRequestForm
import shop.itbug.flutterx.hive.HiveWidget
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.socket.service.DioApiService
import shop.itbug.flutterx.window.android.FlutterXAndroidMigrateWindow
import shop.itbug.flutterx.window.l10n.L10nWindow
import shop.itbug.flutterx.window.logger.LoggerWindow
import shop.itbug.flutterx.window.preview.ImagesPreviewWindow
import shop.itbug.flutterx.window.privacy.PrivacyScanWindow
import shop.itbug.flutterx.window.sp.SpWindow

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

        initDioRequestServer(toolWindow, project) //启动服务器

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
        val l10nWindowContent = instance.createContent(l10nWindow, "L10n", false)
        toolWindow.contentManager.addContent(l10nWindowContent)
        l10nWindowContent.setDisposer(l10nWindow)


        toolWindow.addComposeTab("Cupertino Icons") {
            CupertinoIconsDialog(project)
        }

        toolWindow.addComposeTab("Material Icons") {
            MaterialIconsDialog(project)
        }

        toolWindow.addComposeTab("Enum Migrate") {
            shop.itbug.flutterx.window.migrate.EnumMigrateWindow(project)
        }

    }


    private fun initDioRequestServer(toolWindow: ToolWindow, project: Project) {
        val dioSetting = DioListingUiConfig.getInstance().state ?: DoxListeningSetting()
        if(dioSetting.enableFlutterXDioSocket){
            toolWindow.activate(DioApiService.getInstance().createServerRunner(project, toolWindow))
        }
    }


    override fun shouldBeAvailable(project: Project): Boolean {
        return project.hasPubspecYamlFile() //简单判断一下.
    }

}
