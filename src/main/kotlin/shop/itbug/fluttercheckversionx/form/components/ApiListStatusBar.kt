package shop.itbug.fluttercheckversionx.form.components

import com.intellij.ui.components.JBLabel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.util.Util

class ApiListStatusBar : JBLabel("Done") {
    private val port = PluginStateService.appSetting.serverPort.toInt()
    private val ipList = Util.resolveLocalAddresses().map { it.hostAddress }.joinToString { it }

    init {
        text =
            "Local IP address: $ipList, listening port: $port,connect server code: DdCheckPlugin().init(\$dioInstance,initHost: '${
                Util.resolveLocalAddresses().first().hostAddress
            }', port: $port)"
    }
}