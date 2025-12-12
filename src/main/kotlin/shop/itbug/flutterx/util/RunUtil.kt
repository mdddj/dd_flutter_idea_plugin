package shop.itbug.flutterx.util

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import shop.itbug.flutterx.tools.log

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


    //执行dart run build 命令
    fun dartBuildInBackground(project: Project) {
        commandInBackground(project, "Dart Building", { "Dart Building success" }, { it.message }) {
            val workDirectory = project.guessProjectDir()?.toNioPath()?.toFile()
            val command = GeneralCommandLine("dart", "pub", "run", "build_runner", "build")
            command.workDirectory = workDirectory
            command
        }
    }

    fun commandInBackground(
        project: Project, title: String, onSuccess: (() -> String?)? = null,
        onError: ((error: Throwable) -> String?)? = null,
        generalCommand: () -> GeneralCommandLine,
    ) {
        val task = object : com.intellij.openapi.progress.Task.Backgroundable(project, title) {
            override fun run(p0: ProgressIndicator) {
                val command = generalCommand()
                ExecUtil.execAndGetOutput(command)
            }

            override fun onThrowable(error: Throwable) {

                val msg = onError?.invoke(error)
                msg?.let {
                    project.toastWithError(it)
                }
                super.onThrowable(error)
            }

            override fun onSuccess() {
                val msg = onSuccess?.invoke()
                msg?.let {
                    project.toast(it)
                }
                super.onSuccess()
            }
        }
        task.queue()
    }


    fun runOpenInBackground(e: AnActionEvent,title: String,generateCommand: ()-> GeneralCommandLine) {
        val project = e.getData(CommonDataKeys.PROJECT)!!
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)!!
        commandInBackground(
            project,
            title,
            { null },
            { it.message }
        ) {
            val command = generateCommand()
            command.setWorkDirectory(file.path)
            command
        }
    }
}