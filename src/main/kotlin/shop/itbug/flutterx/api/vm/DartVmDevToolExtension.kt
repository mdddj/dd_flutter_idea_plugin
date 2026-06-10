package shop.itbug.flutterx.api.vm

import com.intellij.openapi.project.Project
import javax.swing.JComponent

interface DartVmDevToolExtension {
    fun getTabTitle(project: Project): String

    fun isAvailable(context: DartVmDevToolContext): Boolean = true

    fun createComponent(context: DartVmDevToolContext): JComponent
}
