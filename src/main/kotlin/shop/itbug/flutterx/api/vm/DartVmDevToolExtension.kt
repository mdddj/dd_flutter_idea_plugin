package shop.itbug.flutterx.api.vm

import javax.swing.JComponent

interface DartVmDevToolExtension {
    fun getTabTitle(context: DartVmDevToolContext): String

    fun isAvailable(context: DartVmDevToolContext): Boolean = true

    fun createComponent(context: DartVmDevToolContext): JComponent
}
