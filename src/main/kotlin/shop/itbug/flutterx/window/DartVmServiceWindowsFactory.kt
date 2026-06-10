package shop.itbug.flutterx.window

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.jetbrains.jewel.bridge.addComposeTab
import shop.itbug.flutterx.api.vm.DartVmDevToolExtensionBean
import shop.itbug.flutterx.common.yaml.hasPubspecYamlFile
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.config.PluginSetting
import shop.itbug.flutterx.setting.vm.DartVmSetting
import shop.itbug.flutterx.widget.AddPackageDialog
import shop.itbug.flutterx.widget.JsonViewerDemo
import shop.itbug.flutterx.window.vm.extension.DartVmDevToolContextImpl

// dart http
class DartVmServiceWindowsFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        toolWindow.component.putClientProperty(DART_VM_WINDOW_CONTENT_INITIALIZED_KEY, true)
        toolWindow.setTitleActions(listOf(OpenDartVmSettingsAction()))
        val context = DartVmDevToolContextImpl(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()
        val config = PluginConfig.getState(project)

        DartVmDevToolExtensionBean.EP_NAME.extensionList.forEach { extensionBean ->
            addDevToolContent(extensionBean, context, contentFactory, toolWindow, config)
        }

        if (System.getenv("DEV") == "true") {
            toolWindow.addComposeTab("Demo") {
                JsonViewerDemo()
            }

            toolWindow.addComposeTab("常见依赖") {
                AddPackageDialog(project)
            }
        }

    }

    override fun shouldBeAvailable(project: Project): Boolean {
        if(!project.hasPubspecYamlFile()){
            return false
        }
        return PluginConfig.getInstance(project).state.enableVmServiceToolWindow
    }

    private fun addDevToolContent(
        extensionBean: DartVmDevToolExtensionBean,
        context: DartVmDevToolContextImpl,
        contentFactory: ContentFactory,
        toolWindow: ToolWindow,
        config: PluginSetting
    ) {
        try {
            val tabId = extensionBean.id
            if (!config.isDartVmDevToolTabVisible(tabId)) {
                return
            }
            val extension = extensionBean.createExtension()
            if (!extension.isAvailable(context)) {
                return
            }
            val title = extension.getTabTitle(context.project)
            val component = extension.createComponent(context)
            val content = contentFactory.createContent(component, title, false)
            if (component is Disposable) {
                content.setDisposer(component)
            }
            toolWindow.contentManager.addContent(content)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            LOG.warn("Failed to create Dart VM dev tool tab from ${extensionBean.implementation}", e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(DartVmServiceWindowsFactory::class.java)
    }

}

private class OpenDartVmSettingsAction : DumbAwareAction(
    { "Dart VM Settings" },
    AllIcons.General.Settings
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, DartVmSetting::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

fun Project.getDartVmWindow() = ToolWindowManager.getInstance(this).getToolWindow(dartVmToolWindowId)
fun Project.refreshDartVmWindowContents() {
    getDartVmWindow()?.let { toolWindow ->
        if (toolWindow.component.getClientProperty(DART_VM_WINDOW_CONTENT_INITIALIZED_KEY) != true) {
            return
        }
        toolWindow.contentManager.removeAllContents(true)
        DartVmServiceWindowsFactory().createToolWindowContent(this, toolWindow)
    }
}

const val dartVmToolWindowId = "FlutterX Dart VM"
private const val DART_VM_WINDOW_CONTENT_INITIALIZED_KEY = "shop.itbug.flutterx.dart.vm.window.content.initialized"
