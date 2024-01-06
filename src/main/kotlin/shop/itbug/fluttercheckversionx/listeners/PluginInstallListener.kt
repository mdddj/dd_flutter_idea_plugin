package shop.itbug.fluttercheckversionx.listeners

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import shop.itbug.fluttercheckversionx.tools.log


class PluginInstallListener : DynamicPluginListener {

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        val changeNotes = pluginDescriptor.changeNotes
        this.log.info("change notes :$changeNotes")
        super.pluginLoaded(pluginDescriptor)
    }
}