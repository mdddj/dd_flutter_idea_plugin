package shop.itbug.fluttercheckversionx.util

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import shop.itbug.fluttercheckversionx.tools.log

object RunUtil {

    fun runFlutterBuildCommand(project: Project) {
        runCommand(project, "Flutter Builder", "flutter pub run build_runner build")
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
    }


    /**
     * 在项目中运行某个命令
     */

    fun runCommand(project: Project, command: String) {
        val commandline = GeneralCommandLine(command.split(" "))
        commandline.workDirectory = project.guessProjectDir()?.toNioPath()?.toFile()
        try {
            ExecUtil.execAndReadLine(commandline)
        } catch (e: Exception) {
            log().warn("执行命令失败:$commandline")
        }
    }

    fun runPubget(project: Project) = runCommand(project, "flutter pub get")
}