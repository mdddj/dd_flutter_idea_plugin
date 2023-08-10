package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object RunUtil {

    fun runCommand(project: Project,title: String,command: String){
        TerminalToolWindowManager.getInstance(project).createLocalShellWidget(project.basePath,title).executeCommand(command)
    }
}