package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

object MyToolWindowTools {

    fun getMyToolWindow(project: Project) : ToolWindow? {
        return ToolWindowManager.getInstance(project = project).getToolWindow("Dio Request")
    }
}