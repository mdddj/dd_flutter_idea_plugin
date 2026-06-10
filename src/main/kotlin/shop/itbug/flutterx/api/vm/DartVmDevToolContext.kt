package shop.itbug.flutterx.api.vm

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import kotlinx.coroutines.flow.StateFlow

interface DartVmDevToolContext {
    val project: Project
    val toolWindow: ToolWindow
    val runningApps: StateFlow<List<DartVmApp>>
}
