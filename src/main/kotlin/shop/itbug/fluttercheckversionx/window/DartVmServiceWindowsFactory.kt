package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.jewel.bridge.addComposeTab
import shop.itbug.fluttercheckversionx.common.yaml.hasPubspecYamlFile
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.widget.AddPackageDialog
import shop.itbug.fluttercheckversionx.widget.JsonViewerDemo
import shop.itbug.fluttercheckversionx.window.vm.*

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
        toolWindow.addComposeTab("Shared Preferences") {
            DartVmSharedPreferencesComponent(project)
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


}

fun Project.getDartVmWindow() = ToolWindowManager.getInstance(this).getToolWindow(dartVmToolWindowId)
const val dartVmToolWindowId = "FlutterX Dart VM"

