package shop.itbug.fluttercheckversionx.common.dart

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.messages.Topic
import com.jetbrains.lang.dart.ide.runner.DartConsoleFilter


/**
 * dart vm 监听器
 */
@Service(Service.Level.PROJECT)
class FlutterXVMService(val project: Project) : Disposable {

    companion object {
        fun getInstance(project: Project) = project.service<FlutterXVMService>()
        val TOPIC = Topic.create("FlutterXVMListener", Listener::class.java)
    }

    /**
     * 添加监听器,自动释放监听器
     */
    fun addListener(listener: Listener) {
        project.messageBus.connect(this).subscribe(TOPIC, listener)
    }

    override fun dispose() {

    }

    interface Listener {

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
    }
}

///需要启用flutter插件,弃坑
class RunConfigListener(val project: Project) : ExecutionListener, ProcessListener {

    private val msgBus = project.messageBus.syncPublisher(FlutterXVMService.TOPIC)


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
            logger<RunConfigListener>().warn("处理数据失败...")
        }
        super.onTextAvailable(event, outputType)
    }

    override fun processTerminated(
        executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int
    ) {
        handler.removeProcessListener(this)
        msgBus.stop(project, executorId, env, exitCode)
    }


}