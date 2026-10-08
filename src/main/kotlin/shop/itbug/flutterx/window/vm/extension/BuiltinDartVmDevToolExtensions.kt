package shop.itbug.flutterx.window.vm.extension

import androidx.compose.runtime.Composable
import com.intellij.mcpserver.McpToolset
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import org.jetbrains.jewel.bridge.JewelComposePanel
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.api.vm.DartVmDevToolExtension
import shop.itbug.flutterx.api.vm.DartVmMcpToolExtension
import shop.itbug.flutterx.mcp.tools.DartVmDriftMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmHiveMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmHttpMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmLoggingMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmMemoryMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmProviderMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmRiverpodMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmSharedPreferencesMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmStatusMcpToolset
import shop.itbug.flutterx.window.vm.DartHttpUI
import shop.itbug.flutterx.window.vm.DartVmHiveComponent
import shop.itbug.flutterx.window.vm.DartVmLoggingComponent
import shop.itbug.flutterx.window.vm.DartVmMemoryComponent
import shop.itbug.flutterx.window.vm.DartVmSharedPreferencesComponent
import shop.itbug.flutterx.window.vm.DartVmStatusComponent
import shop.itbug.flutterx.window.vm.DriftComposeComponent
import shop.itbug.flutterx.window.vm.ProviderComposeComponent
import shop.itbug.flutterx.window.vm.RiverpodComposeComponent
import javax.swing.JComponent

fun ToolWindow.createDartVmComposeComponent(
    content: @Composable () -> Unit
): JComponent = JewelComposePanel(false, {}) {
    content()
}

class DartVmStatusDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Vm"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmStatusComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmStatusMcpToolset()
}

class DartVmMemoryDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Memory"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmMemoryComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmMemoryMcpToolset()
}

class DartVmHttpDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Http Monitor"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartHttpUI(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmHttpMcpToolset()
}

class DartVmLoggingDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Logging"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmLoggingComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmLoggingMcpToolset()
}

class DartVmProviderDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Provider"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            ProviderComposeComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmProviderMcpToolset()
}

class DartVmRiverpodDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Riverpod"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            RiverpodComposeComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmRiverpodMcpToolset()
}

class DartVmSharedPreferencesDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Shared Preferences"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmSharedPreferencesComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmSharedPreferencesMcpToolset()
}

class DartVmHiveDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Hive CE"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmHiveComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmHiveMcpToolset()
}

class DartVmDriftDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Drift DB"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DriftComposeComponent(context)
        }
    }

    override fun createMcpToolset(): McpToolset = DartVmDriftMcpToolset()
}
