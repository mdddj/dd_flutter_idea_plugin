package shop.itbug.fluttercheckversionx.common.dart

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.messages.Topic
import com.jetbrains.lang.dart.ide.runner.DartConsoleFilter
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import vm.VmService
import vm.VmServiceBase

interface FlutterAppVmServiceListener {

    /**
     * 监听控制台,并获取VM URL连接地址
     */
    fun projectOpened(project: Project, env: ProcessEvent, vmUrl: String?, event: FlutterEvent?) {}

    fun stop(
        project: Project, executorId: String, env: ExecutionEnvironment, exitCode: Int
    ) {
    }

    fun onText(project: Project, text: String, event: FlutterEvent?) {}

    fun processStarted(project: Project, executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {}

    fun processFlutterEvent(project: Project, flutterEvent: FlutterEvent, event: ProcessEvent) {}

    fun newVmConnected(vmService: VmService) {}
}

/**
 * dart vm 监听器
 */
@Service(Service.Level.PROJECT)
class FlutterXVMService(val project: Project) : Disposable, FlutterAppVmServiceListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = thisLogger()
    private val connectedVmServices = ConcurrentMap<String, VmService>(3)

    init {
//        project.messageBus.connect(parentDisposable = this).subscribe(TOPIC, this)
    }

    override fun projectOpened(project: Project, env: ProcessEvent, vmUrl: String?, event: FlutterEvent?) {
        if (vmUrl != null) {
            scope.launch {
                try {
                    val vmService = VmServiceBase.connect(vmUrl)
                    connectedVmServices[vmUrl] = vmService
                    project.messageBus.syncPublisher(TOPIC).newVmConnected(vmService)
                } catch (e: Exception) {
                    e.printStackTrace()
                    log.warn("连接dart vm [$vmUrl] 失败:${e}")
                }
            }
        }
        super.projectOpened(project, env, vmUrl, event)
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
    }
}


class RunConfigListener(val project: Project) : ExecutionListener, ProcessListener {

    private val msgBus = project.messageBus.syncPublisher(FlutterXVMService.TOPIC)
    private val log = thisLogger()

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
            val prefix = DartConsoleFilter.OBSERVATORY_LISTENING_ON + "http://"
            val prefix2 = DartConsoleFilter.DART_VM_LISTENING_ON + "http://"
            val prefix3 = "Debug service listening on ws://"
            var url: String? = null
            if (text.startsWith(prefix)) {
                url = "http://" + text.substring(prefix.length)
                event.processHandler.removeProcessListener(this)
            } else if (text.startsWith(prefix2)) {
                url = "http://" + text.substring(prefix2.length)
                event.processHandler.removeProcessListener(this)
            } else if (text.startsWith(prefix3)) {
                url = "ws://" + text.substring(prefix3.length)
                event.processHandler.removeProcessListener(this)
            }
            if (url != null) {
                msgBus.projectOpened(project, event, url, flutterEvent)
            }

            if (flutterEvent != null) {
                msgBus.processFlutterEvent(project, flutterEvent, event)
            }
        } catch (e: Exception) {
            log.warn("解析 vm service url 失败了,", e)
        }
        super.onTextAvailable(event, outputType)
    }

    override fun processTerminated(
        executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int
    ) {
        log.info("控制台退出:${executorId}  $env  $exitCode")
        handler.removeProcessListener(this)
        msgBus.stop(project, executorId, env, exitCode)
    }


}