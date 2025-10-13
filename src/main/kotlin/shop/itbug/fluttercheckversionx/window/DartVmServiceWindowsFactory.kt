package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.jewel.bridge.addComposeTab
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.widget.AddPackageDialog
import shop.itbug.fluttercheckversionx.widget.JsonViewerDemo
import shop.itbug.fluttercheckversionx.window.vm.DartHttpUI
import shop.itbug.fluttercheckversionx.window.vm.DartVmLoggingComponent
import shop.itbug.fluttercheckversionx.window.vm.DartVmStatusComponent
import shop.itbug.fluttercheckversionx.window.vm.ProviderComposeComponent

// dart http
class DartVmServiceWindowsFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        toolWindow.addComposeTab("Vm") {
            DartVmStatusComponent(project)
        }
        toolWindow.addComposeTab("Http Monitor") {
            DartHttpUI(project)
        }
        toolWindow.addComposeTab("Logging") {
            DartVmLoggingComponent(project)
        }
        toolWindow.addComposeTab("Provider") {
            ProviderComposeComponent(project)
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
        return PluginConfig.getInstance(project).state.enableVmServiceToolWindow
    }


}

fun Project.getDartVmWindow() = ToolWindowManager.getInstance(this).getToolWindow(dartVmToolWindowId)
const val dartVmToolWindowId = "FlutterX Dart VM"

