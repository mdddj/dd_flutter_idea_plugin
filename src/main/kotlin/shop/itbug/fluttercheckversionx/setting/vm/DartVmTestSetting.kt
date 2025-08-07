package shop.itbug.fluttercheckversionx.setting.vm

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import vm.VmService
import vm.VmServiceBase
import vm.VmServiceListener
import vm.element.Event
import javax.swing.JComponent

class DartVmTestSetting : Configurable, VmServiceListener {
    private lateinit var myPanel: DialogPanel
    private var url: String = ""
    private var vmService: VmService? = null


    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return "Dart VM Test"
    }

    override fun createComponent(): JComponent? {
        myPanel = panel {
            row("dart vm") {
                textField().bindText({ url }, { url = it })
            }
            row("操作") {
                button("连接") {
                    myPanel.apply()
                    vmService = VmServiceBase.connect(url)
                    vmService?.addVmServiceListener(this@DartVmTestSetting)
                }
            }
        }
        return myPanel
    }

    override fun isModified(): Boolean {
        return myPanel.isModified()
    }

    override fun apply() {
        myPanel.apply()
    }

    override fun connectionOpened() {
        println("连接打开")
    }

    override fun received(streamId: String, event: Event) {
        println("${streamId}收到消息:$event")
    }

    override fun connectionClosed() {
        println("连接关闭")
    }
}