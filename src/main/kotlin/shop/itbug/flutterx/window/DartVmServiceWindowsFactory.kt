package shop.itbug.flutterx.window

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.jetbrains.jewel.bridge.addComposeTab
import shop.itbug.flutterx.api.vm.DartVmDevToolExtension
import shop.itbug.flutterx.common.yaml.hasPubspecYamlFile
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.widget.AddPackageDialog
import shop.itbug.flutterx.widget.JsonViewerDemo
import shop.itbug.flutterx.window.vm.extension.DartVmDevToolContextImpl

// dart http
class DartVmServiceWindowsFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val context = DartVmDevToolContextImpl(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()

        DART_VM_DEV_TOOL_EP.extensionList.forEach { extension ->
            addDevToolContent(extension, context, contentFactory, toolWindow)
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
        extension: DartVmDevToolExtension,
        context: DartVmDevToolContextImpl,
        contentFactory: ContentFactory,
        toolWindow: ToolWindow
    ) {
        try {
            if (!extension.isAvailable(context)) {
                return
            }
            val title = extension.getTabTitle(context)
            val component = extension.createComponent(context)
            val content = contentFactory.createContent(component, title, false)
            if (component is Disposable) {
                content.setDisposer(component)
            }
            toolWindow.contentManager.addContent(content)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            LOG.warn("Failed to create Dart VM dev tool tab from ${extension.javaClass.name}", e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(DartVmServiceWindowsFactory::class.java)
        private val DART_VM_DEV_TOOL_EP =
            ExtensionPointName.create<DartVmDevToolExtension>("shop.itbug.FlutterCheckVersionX.dartVmDevTool")
    }

}

fun Project.getDartVmWindow() = ToolWindowManager.getInstance(this).getToolWindow(dartVmToolWindowId)
const val dartVmToolWindowId = "FlutterX Dart VM"
