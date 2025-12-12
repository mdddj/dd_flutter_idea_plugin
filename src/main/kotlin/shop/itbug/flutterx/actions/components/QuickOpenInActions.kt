package shop.itbug.flutterx.actions.components

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import shop.itbug.flutterx.config.FlutterConfigQuickOpenInCommandDialog
import shop.itbug.flutterx.config.FlutterXGlobalConfigService
import shop.itbug.flutterx.util.RunUtil

class QuickOpenInActions: DefaultActionGroup(), DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<out AnAction?> {
        if(e!=null){
            val list = mutableListOf<AnAction>()
            val setting = FlutterXGlobalConfigService.getInstance().state
            val customOpenIn = setting.quickOpenInCommand
            if (customOpenIn.isNotEmpty()) {
                customOpenIn.forEach { item ->
                    val title = item.title
                    val command = item.command
                    if (title != null && command != null) {
                        list.add(object : DumbAwareAction(title) {
                            override fun actionPerformed(p0: AnActionEvent) {
                                RunUtil.runOpenInBackground(p0, title) {
                                    GeneralCommandLine(command.split(" "))
                                }
                            }
                        })
                    }
                }
            }
            list.add(object : DumbAwareAction("Add Custom Quick Open In","", AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) {
                    val project = e.project
                    if (project != null) {
                        FlutterConfigQuickOpenInCommandDialog(project).show()
                    }
                }
            })
            return list.toTypedArray()
        }
        return super.getChildren(e)
    }

}