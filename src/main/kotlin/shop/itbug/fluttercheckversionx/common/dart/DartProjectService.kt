package shop.itbug.fluttercheckversionx.common.dart

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.messages.Topic
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.window.showDartVmServiceToolWindow
import vm.VmService
import vm.VmServiceBase

interface FlutterXVmStateListener {
    fun newVmConnected(vmService: VmService, url: String)
    fun vmDisconnected(vmService: VmService, url: String)
}

interface FlutterAppVmServiceListener {

    /**
     * 监听控制台,并获取VM URL连接地址
     */
    fun projectOpened(
        project: Project, env: ProcessEvent, vmUrl: String, appId: String, event: FlutterEvent?, listener: RunConfigListener
    ) {
    }

    fun stop(
        project: Project, executorId: String, env: ExecutionEnvironment, exitCode: Int, listener: RunConfigListener
    ) {
    }

    fun onText(project: Project, text: String, event: FlutterEvent?) {}

    fun processStarted(project: Project, executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {}

    fun processFlutterEvent(project: Project, flutterEvent: FlutterEvent, event: ProcessEvent) {}


}

/**
 * dart vm 监听器服务
 */
@Service(Service.Level.PROJECT)
class FlutterXVMService(val project: Project) : Disposable, FlutterAppVmServiceListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = thisLogger()
    private val connectedVmServices = ConcurrentMap<RunConfigListener, VmService>(3)
     val vmServices get() =  connectedVmServices.values.toList()

    init {
//        project.messageBus.connect(parentDisposable = this).subscribe(TOPIC, this)
    }

    override fun projectOpened(
        project: Project,
        env: ProcessEvent,
        vmUrl: String,
        appId: String,
        event: FlutterEvent?,
        listener: RunConfigListener
    ) {
        scope.launch {
            try {
                val vmService = VmServiceBase.connect(vmUrl)
                vmService.putUserData(VmServiceBase.APP_ID_KEY, appId)
                connectedVmServices[listener] = vmService
                project.messageBus.syncPublisher(STATE_TOPIC).newVmConnected(vmService, vmUrl)
                showVmConnectNotification(vmService, vmUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                log.warn("连接dart vm [$vmUrl] 失败:${e}")
            }
        }
    }

    private fun showVmConnectNotification(vm: VmService, url: String) {
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(BALLOON_ID).createNotification(
            "检测 Flutter APP启动", "使用 flutter x 连接 vm service", NotificationType.INFORMATION
        )
        notification.addAction(object : DumbAwareAction("Show In Tool Window") {
            override fun actionPerformed(e: AnActionEvent) {
                showDartVmServiceToolWindow(project)
                notification.expire()
            }
        })
        notification.addAction(object : DumbAwareAction(PluginBundle.get("document")) {
            override fun actionPerformed(e: AnActionEvent) {

            }
        })
        notification.isSuggestionType = true
        notification.notify(project)
    }

    override fun stop(
        project: Project, executorId: String, env: ExecutionEnvironment, exitCode: Int, listener: RunConfigListener
    ) {
        if (connectedVmServices.containsKey(listener)) {
            val vmService = connectedVmServices[listener]
            val url = listener.getUserData(RunConfigListener.VM_URL)
            if (vmService != null && url != null) {
                vmService.disconnect()
                connectedVmServices.remove(listener)
                project.messageBus.syncPublisher(STATE_TOPIC).vmDisconnected(vmService, url)
            }
        }
        super.stop(project, executorId, env, exitCode, listener)
    }

    override fun dispose() {
        connectedVmServices.values.forEach {
            it.disconnect()
        }
        connectedVmServices.clear()
        scope.cancel()
    }

    companion object {
        fun getInstance(project: Project) = project.service<FlutterXVMService>()
        val TOPIC = Topic.create("FlutterXVMListener", FlutterAppVmServiceListener::class.java)
        val STATE_TOPIC = Topic.create<FlutterXVmStateListener>(
            "com.intellij.dart.services.StateListener", FlutterXVmStateListener::class.java
        )
        const val BALLOON_ID = "DartVmServiceListener"
    }
}


class RunConfigListener(val project: Project) : UserDataHolderBase(), ExecutionListener, ProcessListener {

    private val msgBus = project.messageBus.syncPublisher(FlutterXVMService.TOPIC)
    private val log = thisLogger()
    private var wsUrl: String? = null
    private var appId: String? = null

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        println("processStarting $project $executorId  $handler")
        handler.addProcessListener(this)
        msgBus.processStarted(project, executorId, env, handler)
        super.processStarted(executorId, env, handler)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        try {
            val text = event.text.trim()
            val flutterEvent = FlutterEventFactory.formJsonText(text)
            msgBus.onText(project, text, flutterEvent)
            if (flutterEvent != null) {
                val url = flutterEvent.params?.wsUri
                if (url != null) {
                    wsUrl = url
                }
                val appid = flutterEvent.params?.appId
                if(appid != null) {
                    appId = appid
                }
                if (wsUrl != null && appid != null) {
                    if (flutterEvent.event == "app.started") {
                        println("扫描到 url：$wsUrl")
                        msgBus.projectOpened(project, event, wsUrl!!, appid,flutterEvent, this)
                        putUserData(VM_URL, wsUrl)
                    }
                }
            }



            if (flutterEvent != null) {
                msgBus.processFlutterEvent(project, flutterEvent, event)
            }
        } catch (e: Exception) {
            log.info("解析 vm service url 失败了:${event.text}")
        }
        super.onTextAvailable(event, outputType)
    }

    override fun processTerminated(
        executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int
    ) {
        log.info("控制台退出:${executorId}  $env  $exitCode")
        handler.removeProcessListener(this)
        msgBus.stop(project, executorId, env, exitCode, this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is RunConfigListener) {
            val url = other.getUserData(VM_URL)
            if (url == getUserData(VM_URL)) {
                return true
            }
        }
        return super.equals(other)
    }

    companion object {
        val VM_URL = Key.create<String>("DartProjectService.VM_URL")
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + (msgBus?.hashCode() ?: 0)
        result = 31 * result + log.hashCode()
        return result
    }

}