package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object RunUtil {

    fun runFlutterBuildCommand(project: Project) {
        runCommand(project, "Flutter Builder", "flutter pub run build_runner build")
    }

    private fun runFlutterCommandBy233(project: Project, title: String, command: String) {
        ApplicationManager.getApplication().invokeLater {
            val instance = TerminalToolWindowManager.getInstance(project)
            var toolWindow = instance.toolWindow

            //显示窗口
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
            }
        }
    }

    private fun runCommandBy244(project: Project, title: String, command: String) {
        ApplicationManager.getApplication().invokeLater {
            val instance = TerminalToolWindowManager.getInstance(project)
            var toolWindow = instance.toolWindow

            //显示窗口
            if (toolWindow == null) {
                toolWindow =
                    ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
                toolWindow?.show()
            } else {
                if (toolWindow.isAvailable) {
                    toolWindow.show()
                }
            }

            val find = toolWindow.contentManager.findContent(title)
            if (find != null) {
                val tw = TerminalToolWindowManager.findWidgetByContent(find)
                tw?.let { println(tw::class.java) }
                tw?.requestFocus()
                tw?.sendCommandToExecute(command)
                tw?.setCursorVisible(true)
                toolWindow.contentManager.setSelectedContent(find)
            } else {
                toolWindow.activate {
                    val terminal = instance.createShellWidget(project.basePath, title, true, true)
                    terminal.sendCommandToExecute(command)
                }
            }
        }
    }

    fun runCommand(project: Project, title: String, command: String) {
        runCommandBy244(project, title, command)
//        runFlutterCommandBy233(project, title, command)
    }
}