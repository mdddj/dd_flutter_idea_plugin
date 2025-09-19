package shop.itbug.fluttercheckversionx.common.dart

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.messages.Topic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import vm.VmService
import vm.VmServiceBase
import java.util.concurrent.ConcurrentHashMap

val isSupportDartVm = System.getenv("DART_VM") == "true" || Registry.get("flutterx.vm.future.enable").asBoolean()

data class FlutterAppInfo(
    val appId: String,
    val vmUrl: String,
    val deviceId: String,
    val mode: String,
    val events: List<FlutterEvent>
)

/**
 * Flutter应用实例，包含VM服务和相关信息
 */
data class FlutterAppInstance(
    val processHandler: ProcessHandler,
    val vmService: VmService,
    val appInfo: FlutterAppInfo,
    val events: MutableList<FlutterEvent> = mutableListOf()
)


interface FlutterXVmStateListener {
    fun newVmConnected(vmService: VmService, url: String)
    fun vmDisconnected(vmService: VmService, url: String)
}

interface FlutterAppVmServiceListener {

    /**
     * 监听控制台,并获取VM URL连接地址
     */
    fun projectOpened(
        project: Project, env: ProcessEvent, appInfo: FlutterAppInfo, event: FlutterEvent?, listener: ProcessHandler
    ) {
    }

    fun stop(
        project: Project, executorId: String, env: ExecutionEnvironment, exitCode: Int, listener: ProcessHandler
    ) {
    }

    fun onText(project: Project, text: String, event: FlutterEvent?) {}

    fun processStarted(project: Project, executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {}

    fun processFlutterEvent(project: Project, flutterEvent: FlutterEvent, event: ProcessEvent) {}


}

/**
 * Flutter VM服务管理器
 *
 * 主要改进:
 * 1. 使用ConcurrentHashMap正确存储多个Flutter应用实例
 * 2. 每个ProcessHandler对应一个FlutterAppInstance，包含VM服务和应用信息
 * 3. 修复了stop方法中的逻辑错误，正确处理VM服务断开
 * 4. 添加了查询方法，支持按不同条件查找Flutter应用
 * 5. 改进了异常处理和日志记录
 * 6. 支持同时运行多个Flutter应用（如macOS + Chrome）
 *
 * 使用示例:
 * ```kotlin
 * val service = FlutterXVMService.getInstance(project)
 * val runningApps = service.allFlutterApps
 * val macosApp = service.getFlutterAppByAppId("941182c1-d970-493a-98e8-fbe6d80053c1")
 * val chromeApp = service.getFlutterAppByAppId("8bd99081-316b-4ad6-91f8-9d23d33701dd")
 * ```
 */
@Service(Service.Level.PROJECT)
class FlutterXVMService(val project: Project) : Disposable, FlutterAppVmServiceListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
     val log = thisLogger()

    // 使用ConcurrentHashMap存储多个Flutter应用实例
    private val flutterApps = ConcurrentHashMap<ProcessHandler, FlutterAppInstance>()

    // 临时存储ProcessHandler对应的事件，用于构建完整的AppInfo
    private val pendingEvents = ConcurrentHashMap<ProcessHandler, MutableList<FlutterEvent>>()

    // 跟踪已经显示过通知的应用ID，避免重复通知
    private val notifiedApps = ConcurrentHashMap.newKeySet<String>()

    val vmServices: List<VmService> get() = flutterApps.values.map { it.vmService }
    val allFlutterApps: List<FlutterAppInstance> get() = flutterApps.values.toList()

    val isEnable by lazy { isSupportDartVm }

    init {
        log.info("是否启动了 dart vm 的功能:${isEnable}")
        if (isEnable) {
            project.messageBus.connect(parentDisposable = this).subscribe(TOPIC, this)
        }

    }

    override fun processFlutterEvent(project: Project, flutterEvent: FlutterEvent, event: ProcessEvent) {
        val handler = event.processHandler

        // 如果已经有连接的VM服务，直接添加事件
        flutterApps[handler]?.let { appInstance ->
            appInstance.events.add(flutterEvent)

            // 处理app.stop事件，主动断开连接
            if (flutterEvent.event == "app.stop") {
                handleAppStop(handler, appInstance)
            }
            return
        }

        // 否则添加到待处理事件中
        pendingEvents.computeIfAbsent(handler) { mutableListOf() }.add(flutterEvent)

        // 记录重要事件
        when (flutterEvent.event) {
            "app.start" -> log.info("Flutter应用开始启动: ${flutterEvent.params?.appId} 在设备 ${flutterEvent.params?.deviceId} 模式: ${flutterEvent.params?.mode}")
            "app.debugPort" -> {
                log.info("Flutter应用调试端口就绪: ${flutterEvent.params?.appId} - ${flutterEvent.params?.wsUri}")
                // 立即尝试连接VM服务
                tryConnectVmService(handler, flutterEvent)
            }

            "app.started" -> log.info("Flutter应用启动完成: ${flutterEvent.params?.appId}")
            "app.stop" -> log.info("Flutter应用停止: ${flutterEvent.params?.appId}")
        }
    }

    /**
     * 处理应用停止事件
     */
    private fun handleAppStop(handler: ProcessHandler, appInstance: FlutterAppInstance) {
        try {
            log.info("收到应用停止事件，主动断开VM连接: ${appInstance.appInfo.appId}")

            // 通知VM服务断开连接
            project.messageBus.syncPublisher(STATE_TOPIC)
                .vmDisconnected(appInstance.vmService, appInstance.appInfo.vmUrl)

            // 断开VM服务连接
            appInstance.vmService.disconnect()

            // 从映射中移除
            flutterApps.remove(handler)
            updateStateFlow()

            // 清理通知记录
            notifiedApps.remove(appInstance.appInfo.appId)
        } catch (e: Exception) {
            log.warn("处理应用停止事件时发生错误: ${e.message}", e)
        }
    }

    /**
     * 尝试连接VM服务
     * 只有当收到app.debugPort事件时才尝试连接，因为这个事件包含了wsUri
     */
    private fun tryConnectVmService(handler: ProcessHandler, latestEvent: FlutterEvent) {
        log.info("尝试连接VM服务，事件类型: ${latestEvent.event}")

        // 只处理app.debugPort事件，因为它包含VM服务的连接信息
        if (latestEvent.event != "app.debugPort") {
            log.debug("跳过非debugPort事件: ${latestEvent.event}")
            return
        }

        // 检查是否已经连接过
        if (flutterApps.containsKey(handler)) {
            log.info("ProcessHandler已有连接，跳过: ${latestEvent.params?.appId}")
            return
        }

        val vmUrl = extractVmUrl(latestEvent)
        val appId = extractAppId(latestEvent)

        log.info("提取的VM信息 - appId: $appId, vmUrl: $vmUrl")

        if (vmUrl == null || appId == null) {
            log.warn("无法提取VM连接信息 - appId: $appId, vmUrl: $vmUrl")
            return
        }

        // 从之前的事件中查找deviceId和mode信息
        val events = pendingEvents[handler] ?: mutableListOf()
        val deviceId = findDeviceIdFromEvents(events) ?: ""
        val mode = findModeFromEvents(events) ?: "debug"

        log.info("构建AppInfo - deviceId: $deviceId, mode: $mode")

        val appInfo = FlutterAppInfo(
            appId = appId,
            vmUrl = vmUrl,
            deviceId = deviceId,
            mode = mode,
            events = events.toList()
        )

        connectToVmService(handler, appInfo)
    }

    /**
     * 从事件列表中查找设备ID
     */
    private fun findDeviceIdFromEvents(events: List<FlutterEvent>): String? {
        return events.find { it.event == "app.start" }?.params?.deviceId
    }

    /**
     * 从事件列表中查找运行模式
     */
    private fun findModeFromEvents(events: List<FlutterEvent>): String? {
        return events.find { it.event == "app.start" }?.params?.mode
    }


    /**
     * 连接到VM服务
     */
    private fun connectToVmService(handler: ProcessHandler, appInfo: FlutterAppInfo) {
        // 检查是否已经连接过这个应用
        if (flutterApps.containsKey(handler)) {
            log.info("应用已连接，跳过重复连接: ${appInfo.appId}")
            return
        }

        // 检查是否已经有相同appId的应用连接
        val existingApp = flutterApps.values.find { it.appInfo.appId == appInfo.appId }
        if (existingApp != null) {
            log.info("相同appId的应用已存在，跳过连接: ${appInfo.appId}")
            return
        }

        scope.launch {
            try {
                val vmService = VmServiceBase.connect(appInfo.vmUrl)
                vmService.putUserData(VmServiceBase.APP_ID_KEY, appInfo.appId)
                vmService.putUserData(VmServiceBase.APP_INFO, appInfo)

                // 创建Flutter应用实例
                vmService.updateMainIsolateId()
                vmService.startListenStreams()
                val appInstance = FlutterAppInstance(
                    processHandler = handler,
                    vmService = vmService,
                    appInfo = appInfo,
                    events = (pendingEvents.remove(handler) ?: mutableListOf())
                )

                flutterApps[handler] = appInstance
                updateStateFlow()

                // 存储VM URL到ProcessHandler的用户数据中
                handler.putUserData(RunConfigListener.VM_URL, appInfo.vmUrl)

                project.messageBus.syncPublisher(STATE_TOPIC).newVmConnected(vmService, appInfo.vmUrl)

                // 检查工具窗口是否显示，如果没有显示则显示通知
//                val isShow = dartVmServiceWindowIsShow(project)
//                if (!isShow && !notifiedApps.contains(appInfo.appId)) {
//                    log.info("工具窗口未显示且未通知过，显示VM连接通知: ${appInfo.appId}")
//                    showVmConnectNotification(vmService, appInfo.vmUrl, appInfo.appId)
//                    notifiedApps.add(appInfo.appId)
//                } else {
//                    log.info("工具窗口已显示或已通知过，跳过通知: ${appInfo.appId}")
//                }

                log.info("成功连接到Flutter应用: ${appInfo.appId} - ${appInfo.vmUrl}")

            } catch (e: Exception) {
                log.warn("连接dart vm [$appInfo] 失败: ${e.message}", e)
                // 连接失败时清理待处理事件
                pendingEvents.remove(handler)

                // 显示错误通知
                val notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(BALLOON_ID)
                    .createNotification(
                        "Flutter VM连接失败",
                        "无法连接到应用 ${appInfo.appId}: ${e.message}",
                        NotificationType.WARNING
                    )
                notification.notify(project)
            }
        }
    }

    override fun projectOpened(
        project: Project,
        env: ProcessEvent,
        appInfo: FlutterAppInfo,
        event: FlutterEvent?,
        listener: ProcessHandler
    ) {
        connectToVmService(listener, appInfo)
    }

    /**
     * 从app.debugPort事件中提取VM服务URL
     */
    private fun extractVmUrl(event: FlutterEvent): String? {
        return if (event.event == "app.debugPort") {
            event.params?.wsUri
        } else null
    }

    /**
     * 从app.debugPort事件中提取应用ID
     */
    private fun extractAppId(event: FlutterEvent): String? {
        return if (event.event == "app.debugPort") {
            event.params?.appId
        } else null
    }

    override fun stop(
        project: Project, executorId: String, env: ExecutionEnvironment, exitCode: Int, listener: ProcessHandler
    ) {
        // 查找对应的Flutter应用实例
        val appInstance = flutterApps[listener]
        if (appInstance != null) {
            try {
                // 通知VM服务断开连接
                project.messageBus.syncPublisher(STATE_TOPIC)
                    .vmDisconnected(appInstance.vmService, appInstance.appInfo.vmUrl)

                // 断开VM服务连接
                appInstance.vmService.cancelListenStreams()
                appInstance.vmService.disconnect()

                // 从映射中移除
                flutterApps.remove(listener)
                updateStateFlow()

                // 清理通知记录
                notifiedApps.remove(appInstance.appInfo.appId)

                log.info("Flutter应用已停止: ${appInstance.appInfo.appId} - ${appInstance.appInfo.vmUrl}")
            } catch (e: Exception) {
                log.warn("停止Flutter应用时发生错误: ${e.message}", e)
            }
        } else {
            // 清理可能存在的待处理事件
            pendingEvents.remove(listener)
            log.debug("没有找到对应的Flutter应用实例: $executorId")
        }
    }

    /**
     * 获取指定ProcessHandler对应的Flutter应用实例
     */
    fun getFlutterApp(handler: ProcessHandler): FlutterAppInstance? = flutterApps[handler]

    /**
     * 根据应用ID获取Flutter应用实例
     */
    fun getFlutterAppByAppId(appId: String): FlutterAppInstance? =
        flutterApps.values.find { it.appInfo.appId == appId }

    /**
     * 根据VM URL获取Flutter应用实例
     */
    fun getFlutterAppByVmUrl(vmUrl: String): FlutterAppInstance? =
        flutterApps.values.find { it.appInfo.vmUrl == vmUrl }

    /**
     * 获取当前运行的Flutter应用数量
     */
    fun getRunningAppsCount(): Int = flutterApps.size

    /**
     * 获取所有运行中的应用信息（用于调试）
     */
    fun getRunningAppsInfo(): List<String> =
        flutterApps.values.map { "${it.appInfo.appId} (${it.appInfo.deviceId}) - ${it.appInfo.vmUrl}" }




    // ---- compose
    // 1. 创建一个私有的、可变的 StateFlow
    //    它持有当前所有运行的应用实例列表，并以空列表作为初始值
    private val _runningApps = MutableStateFlow<List<FlutterAppInstance>>(emptyList())

    // 2. 暴露一个公开的、只读的 StateFlow，供外部（UI层）订阅
    val runningApps = _runningApps.asStateFlow()

    private fun updateStateFlow() {
        _runningApps.value = flutterApps.values.toList()
    }


    override fun dispose() {
        // 断开所有VM服务连接
        flutterApps.values.forEach { appInstance ->
            try {
                appInstance.vmService.disconnect()
            } catch (e: Exception) {
                log.warn("断开VM服务连接时发生错误: ${e.message}", e)
            }
        }

        // 清理所有数据
        flutterApps.clear()
        pendingEvents.clear()
        notifiedApps.clear()
        updateStateFlow()

        // 取消协程作用域
        scope.cancel()
    }

    companion object {
        fun getInstance(project: Project): FlutterXVMService = project.service<FlutterXVMService>()
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

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        log.info("Flutter进程启动: $executorId")
        handler.addProcessListener(this)
        msgBus.processStarted(project, executorId, env, handler)
        super.processStarted(executorId, env, handler)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        try {
            val text = event.text.trim()
            if (text.isBlank()) return

            // 只记录关键事件
            if (text.contains("app.debugPort")) {
                log.info("🎯 收到关键的app.debugPort事件: $text")
            }

            val flutterEvent = FlutterEventFactory.formJsonText(text)

            // 通知文本处理
            msgBus.onText(project, text, flutterEvent)

            // 如果解析出Flutter事件，则处理该事件
            if (flutterEvent != null) {
                log.info("成功解析Flutter事件: ${flutterEvent.event} - ${flutterEvent.params}")
                msgBus.processFlutterEvent(project, flutterEvent, event)
            }
        } catch (e: Exception) {
            log.warn("解析Flutter事件失败: ${event.text.take(100)}...", e)
        }
        super.onTextAvailable(event, outputType)
    }

    override fun processTerminated(
        executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int
    ) {
        log.info("Flutter进程终止: $executorId, 退出码: $exitCode")
        handler.removeProcessListener(this)
        msgBus.stop(project, executorId, env, exitCode, handler)
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
        result = 31 * result + msgBus.hashCode()
        return result
    }

}