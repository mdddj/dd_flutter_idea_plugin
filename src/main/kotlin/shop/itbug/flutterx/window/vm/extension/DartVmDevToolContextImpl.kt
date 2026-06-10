package shop.itbug.flutterx.window.vm.extension

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import kotlinx.coroutines.flow.StateFlow
import shop.itbug.flutterx.api.vm.DartVmApp
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.common.dart.FlutterXVMService

internal class DartVmDevToolContextImpl(
    override val project: Project,
    override val toolWindow: ToolWindow
) : DartVmDevToolContext {
    override val runningApps: StateFlow<List<DartVmApp>> =
        FlutterXVMService.getInstance(project).runningApps
}
