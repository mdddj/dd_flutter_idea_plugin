package shop.itbug.fluttercheckversionx.window.flutter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.widget.FlutterWidgetTreeWidget
import vm.*
import vm.element.Event
import javax.swing.JButton

class WidgetTreeWindow(val project: Project) : BorderLayoutPanel(), Disposable, VmServiceListener {
    val tree = FlutterWidgetTreeWidget(project, "test-group")
    val textField = JBTextField("ws://127.0.0.1:51279/O7dUpbtsCdM=/ws")
    val connectButton = JButton("连接 vm")
    var vmService: VmService? = null
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val detailPanel = FlutterNodeDetailPanel()

    val spPanel = OnePixelSplitter().apply {
        splitterProportionKey = "FlutterWidgetTreeWidget"
        firstComponent = tree.scroll()
        secondComponent = detailPanel.scroll()
    }


    init {
        initUI()
        connectButton.addActionListener { connectToVmServer() }
        Disposer.register(this, tree)
    }

    private fun connectToVmServer() {
        val url = textField.text
        vmService = VmServiceBase.connect(url, this)
        scope.launch {
            val mainIos = vmService?.mainIsolates()
            if (mainIos != null) {
                val isolateId = mainIos.getId()!!
                // 将 vmService 和 isolateId 传递给 tree widget
                tree.setVmService(vmService, isolateId)

                val response =
                    vmService?.getRootWidgetTree(
                        isolateId,
                        tree.group,
                        true,
                        withPreviews = true,
                        fullDetails = true
                    )
                response?.let { tree.updateTree(it) }
            }
        }
    }

    private fun initUI() {

        addToCenter(spPanel)

        addToTop(
            HorizontalBox().apply {
                add(textField)
                add(connectButton)
            }
        )
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


    inner class FlutterNodeDetailPanel : BorderLayoutPanel() {

    }
}
