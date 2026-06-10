package shop.itbug.flutterx.window.vm.extension

import androidx.compose.runtime.Composable
import com.intellij.openapi.wm.ToolWindow
import org.jetbrains.jewel.bridge.JewelComposePanel
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.api.vm.DartVmDevToolExtension
import shop.itbug.flutterx.window.vm.DartHttpUI
import shop.itbug.flutterx.window.vm.DartVmHiveComponent
import shop.itbug.flutterx.window.vm.DartVmLoggingComponent
import shop.itbug.flutterx.window.vm.DartVmMemoryComponent
import shop.itbug.flutterx.window.vm.DartVmSharedPreferencesComponent
import shop.itbug.flutterx.window.vm.DartVmStatusComponent
import shop.itbug.flutterx.window.vm.DriftComposeComponent
import shop.itbug.flutterx.window.vm.ProviderComposeComponent
import javax.swing.JComponent

private fun ToolWindow.createDartVmComposeComponent(
    content: @Composable () -> Unit
): JComponent = JewelComposePanel(true, {}) {
    content()
}

class DartVmStatusDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Vm"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmStatusComponent(context.project)
        }
    }
}

class DartVmMemoryDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Memory"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmMemoryComponent(context.project)
        }
    }
}

class DartVmHttpDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Http Monitor"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartHttpUI(context.project)
        }
    }
}

class DartVmLoggingDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Logging"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmLoggingComponent(context.project)
        }
    }
}

class DartVmProviderDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Provider"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            ProviderComposeComponent(context.project)
        }
    }
}

class DartVmSharedPreferencesDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Shared Preferences"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmSharedPreferencesComponent(context.project)
        }
    }
}

class DartVmHiveDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Hive CE"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmHiveComponent(context.project)
        }
    }
}

class DartVmDriftDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(context: DartVmDevToolContext): String = "Drift DB"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DriftComposeComponent(context.project)
        }
    }
}
