package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object RunUtil {

    fun runCommand(project: Project, title: String, command: String) {
        ApplicationManager.getApplication().invokeLater {
            val instance = TerminalToolWindowManager.getInstance(project)
            var toolWindow = instance.toolWindow
            if (toolWindow == null) {
                toolWindow =
                    ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
                toolWindow?.show()
            } else {
                if (toolWindow.isAvailable) {
                    toolWindow.show()
                }
            }
            toolWindow.activate {
                // 241--
                instance.createLocalShellWidget(project.basePath, title).executeCommand(command)
                // 241+
//                instance.createShellWidget(project.basePath, title, true, true).sendCommandToExecute(command) //241+
            }
        }
    }
}