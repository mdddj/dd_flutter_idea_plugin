package shop.itbug.fluttercheckversionx.window.flutter

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVmStateListener
import shop.itbug.fluttercheckversionx.widget.FlutterTreeComponent
import vm.VmService
import javax.swing.SwingUtilities

class WidgetTreeWindow(val project: Project) : BorderLayoutPanel(), Disposable,
    FlutterXVmStateListener {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val log = thisLogger()
    val appService get() = FlutterXVMService.getInstance(project)
    val vmServices get() = appService.vmServices

    private val tab = JBTabbedPane()

    init {
        initUI()
        SwingUtilities.invokeLater {
            vmServices.forEach { vm ->
                val comp = FlutterTreeComponent(project, vm.appId, vm)
                Disposer.register(this, comp)
                tab.add(vm.appInfo.deviceId, comp)
            }
        }

        project.messageBus.connect(this).subscribe(FlutterXVMService.STATE_TOPIC, this)
    }


    private fun initUI() {
        addToCenter(tab)
    }

    override fun dispose() {

        scope.cancel()
    }

    override fun newVmConnected(vmService: VmService, url: String) {
        SwingUtilities.invokeLater {
            val comp = FlutterTreeComponent(project, vmService.appId, vmService)
            Disposer.register(this, comp)
            tab.add(vmService.appInfo.deviceId, comp)
            tab.repaint()
            tab.invalidate()
        }
    }

    override fun vmDisconnected(vmService: VmService, url: String) {
        log.info("监听到 dart vm 断开连接，即将移除 tab")
        val find = tab.components.filterIsInstance<FlutterTreeComponent>().find { it.isEq(vmService) }
        if(find != null) {
            tab.remove(find)
        }

    }


}
