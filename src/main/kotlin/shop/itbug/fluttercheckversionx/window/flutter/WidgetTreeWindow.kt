package shop.itbug.fluttercheckversionx.window.flutter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.widget.FlutterWidgetTreeWidget
import vm.VmServiceListener
import vm.element.Event
import javax.swing.SwingUtilities

class WidgetTreeWindow(val project: Project) : BorderLayoutPanel(), Disposable, VmServiceListener {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val appService get() = FlutterXVMService.getInstance(project)
    val vmServices get() = appService.vmServices

    private val tab = JBTabbedPane()

    init {
        initUI()
        SwingUtilities.invokeLater {
            vmServices.forEach { vm ->
                tab.add(JBLabel(""), FlutterWidgetTreeWidget(project, vm.appId, vm))
            }
        }
    }


    private fun initUI() {
        addToCenter(tab)
    }

    override fun dispose() {

        scope.cancel()
    }

    override fun connectionOpened() {
        println("连接打开")
    }

    override fun received(streamId: String, event: Event) {}

    override fun connectionClosed() {
        println("连接断开")
    }


}
