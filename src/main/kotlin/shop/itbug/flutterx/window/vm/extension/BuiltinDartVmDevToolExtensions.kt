package shop.itbug.flutterx.window.vm.extension

import androidx.compose.runtime.Composable
import com.intellij.openapi.project.Project
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
import shop.itbug.flutterx.window.vm.RiverpodComposeComponent
import javax.swing.JComponent

fun ToolWindow.createDartVmComposeComponent(
    content: @Composable () -> Unit
): JComponent = JewelComposePanel(false, {}) {
    content()
}

class DartVmStatusDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Vm"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmStatusComponent(context)
        }
    }
}

class DartVmMemoryDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Memory"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmMemoryComponent(context)
        }
    }
}

class DartVmHttpDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Http Monitor"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartHttpUI(context)
        }
    }
}

class DartVmLoggingDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Logging"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmLoggingComponent(context)
        }
    }
}

class DartVmProviderDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Provider"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            ProviderComposeComponent(context)
        }
    }
}

class DartVmRiverpodDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Riverpod"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            RiverpodComposeComponent(context)
        }
    }
}

class DartVmSharedPreferencesDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Shared Preferences"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmSharedPreferencesComponent(context)
        }
    }
}

class DartVmHiveDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Hive CE"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DartVmHiveComponent(context)
        }
    }
}

class DartVmDriftDevToolExtension : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "Drift DB"

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return context.toolWindow.createDartVmComposeComponent {
            DriftComposeComponent(context)
        }
    }
}
