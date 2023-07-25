package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalView

object RunUtil {

    fun runCommand(project: Project,title: String,command: String){
        TerminalView.getInstance(project).createLocalShellWidget(project.basePath,title).executeCommand(command)
    }
}