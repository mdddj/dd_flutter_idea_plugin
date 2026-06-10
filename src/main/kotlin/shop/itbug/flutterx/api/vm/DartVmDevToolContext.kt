package shop.itbug.flutterx.api.vm

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow

interface DartVmDevToolContext {
    val project: Project
    val toolWindow: ToolWindow

    fun getRunningApps(): List<DartVmApp>
}
