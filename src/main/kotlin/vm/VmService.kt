package vm


import com.google.gson.*
import com.intellij.openapi.diagnostic.thisLogger
import fleet.multiplatform.shims.ConcurrentHashMap
import io.ktor.websocket.*
import io.ktor.websocket.Frame
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vm.consumer.*
import vm.element.*
import vm.log.LoggingController
import vm.logging.Logging
import vm.network.DartNetworkMonitor
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VmService : VmServiceBase() {
    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setStrictness(Strictness.LENIENT)
        .create()
    private val logger = thisLogger()
    val appId get() = getUserData(APP_ID_KEY)!!
    val appInfo get() = getUserData(APP_INFO)!!
    val logController by lazy {
        LoggingController(this, coroutineScope)
    }

    private val listenStreamsEvents = arrayOf(
        *EventKind.entries.map { it.name }.toTypedArray()
    )


    var dartHttpMonitor: MutableStateFlow<DartNetworkMonitor?> = MutableStateFlow(null)
    private var _mainIsolateId: String? = null

    fun getMainIsolateId(): String {
        if (_mainIsolateId != null) return _mainIsolateId!!
        return ""
    }

    suspend fun getMainIsolates(): IsolateRef? {
        return mainIsolates()
    }

    suspend fun updateMainIsolateId() {
        _mainIsolateId = mainIsolates()?.getId()
    }

    val dartHttpIsMonitoring = MutableStateFlow(false)
    val getAllRequests get() = dartHttpMonitor.value?.getAllRequests() ?: emptyList()

    suspend fun startMonitoring(
        intervalMs: Long = 1000L,
        listener: DartNetworkMonitor.NetworkRequestListener? = null
    ): Job? {
        if (dartHttpMonitor.value == null) {
            dartHttpMonitor.value = DartNetworkMonitor(
                vmService = this,
                scope = coroutineScope,
            )
        }
        if (listener != null) {
            dartHttpMonitor.value?.addListener(listener)
        }
        dartHttpMonitor.value?.startMonitoring(intervalMs)
        dartHttpIsMonitoring.value = true

        return dartHttpMonitor.value?.taskJob
    }

    fun destroyHttpMonitor() {
        dartHttpIsMonitoring.value = false
        dartHttpMonitor.value?.destroy()
        dartHttpMonitor.value = null
    }

    fun runInScope(action: suspend VmService.() -> Unit) {
        coroutineScope.launch {
            action.invoke(this@VmService)
        }
    }


    companion object {
        const val DEBUG_STREAM_ID = "Debug"

        const val EXTENSION_STREAM_ID = "Extension"

        const val GC_STREAM_ID = "GC"

        const val HEAPSNAPSHOT_STREAM_ID = "HeapSnapshot"

        const val ISOLATE_STREAM_ID = "Isolate"

        const val LOGGING_STREAM_ID = "Logging"

        const val PROFILER_STREAM_ID = "Profiler"

        const val SERVICE_STREAM_ID = "Service"

        const val STDERR_STREAM_ID = "Stderr"

        const val STDOUT_STREAM_ID = "Stdout"

        const val TIMELINE_STREAM_ID = "Timeline"

        const val VM_STREAM_ID = "VM"

        /**
         * 此客户端支持的协议的主版本号。
         */
        const val versionMajor = 4

        /**
         * 此客户端支持的协议的次版本号。
         */
        const val versionMinor = 3


    }


    //监听 dart vm 流
    fun startListenStreams() {
        coroutineScope.launch {
            listenStreamsEvents.map(::streamListen)
            addEventListener(hotRestartListener)
        }
    }

    fun cancelListenStreams() {
        listenStreamsEvents.forEach {
            coroutineScope.launch { streamCancel(it) }
            removeEventListener(hotRestartListener)
        }
    }

    private val _vmEvents = MutableSharedFlow<EventKind>()
    val vmEvents: SharedFlow<EventKind> = _vmEvents

    //监听dart vm 热重启
    private val hotRestartListener = object : VmEventListener {
        override fun onVmEvent(streamId: String, event: Event) {
            runInScope {
                _vmEvents.emit(event.getKind())
            }
            if (streamId != ISOLATE_STREAM_ID) {
                return
            }
            val eventIsolateId = event.getIsolate()?.getId()

            when (event.getKind()) {
                EventKind.IsolateExit -> {
                    if (eventIsolateId != null && eventIsolateId == getMainIsolateId()) {
                        logger.info("🔥 主 Isolate (id: $eventIsolateId) 正在退出，这很可能是热重启的第一步。")
                    }
                }

                EventKind.IsolateStart -> {
                    val newIsolateName = event.getIsolate()?.getName()
                    if (newIsolateName == "main") {
                        logger.info("✅ 监听到热重启成功！新的主 Isolate (name: $newIsolateName, id: $eventIsolateId) 已启动。")
                        runInScope {
                            updateMainIsolateId()
                        }
                    }
                }

                EventKind.IsolateReload -> {
                    if (eventIsolateId != null && eventIsolateId == getMainIsolateId()) {
                        logger.info("🔄 监听到热重载 (Hot Reload) on isolate: $eventIsolateId")
                    }
                }

                else -> {
                }
            }
        }

    }


    fun addBreakpoint(isolateId: String, scriptId: String, line: Int, consumer: AddBreakpointConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("scriptId", scriptId)
        params.addProperty("line", line)
        request("addBreakpoint", params, consumer)
    }

    fun addBreakpoint(isolateId: String, scriptId: String, line: Int, column: Int?, consumer: AddBreakpointConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("scriptId", scriptId)
        params.addProperty("line", line)
        if (column != null) params.addProperty("column", column)
        request("addBreakpoint", params, consumer)
    }

    private val eventListeners = mutableListOf<VmEventListener>()

    interface VmEventListener {
        fun onVmEvent(streamId: String, event: Event)
    }

    fun addEventListener(listener: VmEventListener) {
        println("添加 vm event 事件监听器")
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: VmEventListener) {
        eventListeners.remove(listener)
    }

    private fun forwardEventToCustomListeners(streamId: String, event: Event) {
        // 转发给自定义事件监听器
        for (listener in ArrayList(eventListeners)) {
            try {
                listener.onVmEvent(streamId, event)
            } catch (e: Exception) {
                e.printStackTrace()
                println("处理自定义事件监听器异常: ${e.message}")
            }
        }
    }

    override fun processMessage(jsonText: String?) {
        super.processMessage(jsonText)
        if (jsonText != null && jsonText.isNotEmpty()) {
            try {
                val json = gson.fromJson(jsonText, JsonObject::class.java)
                if (json.has("method") && json.get("method").asString == "streamNotify") {
                    val params = json.getAsJsonObject("params")
                    val streamId = params.get("streamId").asString
                    val eventJson = params.getAsJsonObject("event")
                    val event = Event(eventJson)
                    forwardEventToCustomListeners(streamId, event)
                }
            } catch (e: Exception) {
                // 忽略解析错误，不影响正常流程
            }
        }
    }

    suspend fun listenData() {
        var retryCount = 0
        val maxRetries = 3
        while (retryCount < maxRetries) {
            try {
                myWebSocketSession?.let { session ->
                    Logging.getLogger().logInformation("开始监听数据 (尝试 ${retryCount + 1})")

                    for (frame in session.incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val message = frame.readText()
                                processMessage(message)
                                retryCount = 0  // 重置重试计数
                            }

                            is Frame.Close -> {
                                Logging.getLogger().logInformation("WebSocket 正常关闭")
                                return
                            }

                            else -> {
                                Logging.getLogger().logInformation("收到其他类型帧: ${frame.frameType}")
                            }
                        }
                    }
                }
                break
            } catch (e: CancellationException) {
                Logging.getLogger().logInformation("监听被取消")
                throw e
            } catch (e: Exception) {
                retryCount++
                Logging.getLogger().logError("监听异常 (尝试 $retryCount/$maxRetries): ${e.message}", e)

                if (retryCount < maxRetries) {
                    Logging.getLogger().logInformation("等待 ${retryCount * 1000}ms 后重试...")
                    delay(retryCount * 1000L)
                } else {
                    Logging.getLogger().logError("达到最大重试次数，停止监听")
                }
            }
        }
    }

    /**
     * [addBreakpointAtEntry] RPC 用于在某个函数的入口点添加断点。
     */
    fun addBreakpointAtEntry(isolateId: String, functionId: String, consumer: AddBreakpointAtEntryConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("functionId", functionId)
        request("addBreakpointAtEntry", params, consumer)
    }

    /**
     * [addBreakpoint] RPC 用于在脚本的特定行添加断点。
     * 当脚本尚未分配 ID 时，此 RPC 很有用，例如，如果脚本位于尚未加载的延迟库中。
     */
    fun addBreakpointWithScriptUri(
        isolateId: String,
        scriptUri: String,
        line: Int,
        consumer: AddBreakpointWithScriptUriConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("scriptUri", scriptUri)
        params.addProperty("line", line)
        request("addBreakpointWithScriptUri", params, consumer)
    }

    /**
     * [addBreakpoint] RPC 用于在脚本的特定行添加断点。
     * 当脚本尚未分配 ID 时，此 RPC 很有用，例如，如果脚本位于尚未加载的延迟库中。
     * @param column 此参数是可选的，可以为 null。
     */
    fun addBreakpointWithScriptUri(
        isolateId: String,
        scriptUri: String,
        line: Int,
        column: Int?,
        consumer: AddBreakpointWithScriptUriConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("scriptUri", scriptUri)
        params.addProperty("line", line)
        if (column != null) params.addProperty("column", column)
        request("addBreakpointWithScriptUri", params, consumer)
    }

    /**
     * 清除所有 CPU 性能分析样本。
     */
    fun clearCpuSamples(isolateId: String, consumer: ClearCpuSamplesConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("clearCpuSamples", params, consumer)
    }

    /**
     * 清除所有 VM 时间线事件。
     */
    fun clearVMTimeline(consumer: SuccessConsumer) {
        val params = JsonObject()
        request("clearVMTimeline", params, consumer)
    }

    /**
     * [evaluate] RPC 用于在某个目标的上下文中计算表达式。
     */
    fun evaluate(isolateId: String, targetId: String, expression: String, consumer: EvaluateConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("targetId", targetId)
        params.addProperty("expression", removeNewLines(expression))
        request("evaluate", params, consumer)
    }

    /**
     * [evaluate] RPC 用于在某个目标的上下文中计算表达式。
     * @param scope 此参数是可选的，可以为 null。
     * @param disableBreakpoints 此参数是可选的，可以为 null。
     */
    fun evaluate(
        isolateId: String,
        targetId: String,
        expression: String,
        scope: Map<String, String>?,
        disableBreakpoints: Boolean?,
        consumer: EvaluateConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("targetId", targetId)
        params.addProperty("expression", removeNewLines(expression))
        if (scope != null) params.add("scope", convertMapToJsonObject(scope))
        if (disableBreakpoints != null) params.addProperty("disableBreakpoints", disableBreakpoints)
        request("evaluate", params, consumer)
    }

    /**
     * [evaluateInFrame] RPC 用于在特定堆栈帧的上下文中计算表达式。
     * [frameIndex] 是所需帧的索引，索引 [0] 表示顶部（最近的）帧。
     */
    fun evaluateInFrame(isolateId: String, frameIndex: Int, expression: String, consumer: EvaluateInFrameConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("frameIndex", frameIndex)
        params.addProperty("expression", removeNewLines(expression))
        request("evaluateInFrame", params, consumer)
    }

    /**
     * [evaluateInFrame] RPC 用于在特定堆栈帧的上下文中计算表达式。
     * [frameIndex] 是所需帧的索引，索引 [0] 表示顶部（最近的）帧。
     * @param scope 此参数是可选的，可以为 null。
     * @param disableBreakpoints 此参数是可选的，可以为 null。
     */
    fun evaluateInFrame(
        isolateId: String,
        frameIndex: Int,
        expression: String,
        scope: Map<String, String>?,
        disableBreakpoints: Boolean?,
        consumer: EvaluateInFrameConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("frameIndex", frameIndex)
        params.addProperty("expression", removeNewLines(expression))
        if (scope != null) params.add("scope", convertMapToJsonObject(scope))
        if (disableBreakpoints != null) params.addProperty("disableBreakpoints", disableBreakpoints)
        request("evaluateInFrame", params, consumer)
    }

    /**
     * [getAllocationProfile] RPC 用于检索给定隔离区的分配信息。
     * @param reset 此参数是可选的，可以为 null。
     * @param gc 此参数是可选的，可以为 null。
     */
    fun getAllocationProfile(isolateId: String, reset: Boolean?, gc: Boolean?, consumer: GetAllocationProfileConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        if (reset != null) params.addProperty("reset", reset)
        if (gc != null) params.addProperty("gc", gc)
        request("getAllocationProfile", params, consumer)
    }

    /**
     * [getAllocationProfile] RPC 用于检索给定隔离区的分配信息。
     */
    fun getAllocationProfile(isolateId: String, consumer: GetAllocationProfileConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getAllocationProfile", params, consumer)
    }

    /**
     * [getAllocationTraces] RPC 允许检索特定类型对象的分配跟踪（参见 setTraceClassAllocation）。
     * 仅报告在时间范围 <code>[timeOriginMicros, timeOriginMicros + timeExtentMicros]</code>
     * [timeOriginMicros, timeOriginMicros + timeExtentMicros] 内收集的样本。
     */
    fun getAllocationTraces(isolateId: String, consumer: CpuSamplesConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getAllocationTraces", params, consumer)
    }

    /**
     * [getAllocationTraces] RPC 允许检索特定类型对象的分配跟踪（参见 setTraceClassAllocation）。
     * 仅报告在时间范围 <code>[timeOriginMicros, timeOriginMicros + timeExtentMicros]</code>
     * [timeOriginMicros, timeOriginMicros + timeExtentMicros] 内收集的样本。
     * @param timeOriginMicros 此参数是可选的，可以为 null。
     * @param timeExtentMicros 此参数是可选的，可以为 null。
     * @param classId 此参数是可选的，可以为 null。
     */
    fun getAllocationTraces(
        isolateId: String,
        timeOriginMicros: Int?,
        timeExtentMicros: Int?,
        classId: String?,
        consumer: CpuSamplesConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        if (timeOriginMicros != null) params.addProperty("timeOriginMicros", timeOriginMicros)
        if (timeExtentMicros != null) params.addProperty("timeExtentMicros", timeExtentMicros)
        if (classId != null) params.addProperty("classId", classId)
        request("getAllocationTraces", params, consumer)
    }

    /**
     * [getClassList] RPC 用于检索包含基于隔离区 [isolateId] 的所有类的 [ClassList]。
     */
    fun getClassList(isolateId: String, consumer: GetClassListConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getClassList", params, consumer)
    }

    /**
     * [getCpuSamples] RPC 用于检索 CPU 分析器收集的样本。
     * 仅报告在时间范围 <code>[timeOriginMicros, timeOriginMicros +
     * timeExtentMicros]</code>[timeOriginMicros, timeOriginMicros + timeExtentMicros] 内收集的样本。
     */
    fun getCpuSamples(
        isolateId: String,
        timeOriginMicros: Int,
        timeExtentMicros: Int,
        consumer: GetCpuSamplesConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("timeOriginMicros", timeOriginMicros)
        params.addProperty("timeExtentMicros", timeExtentMicros)
        request("getCpuSamples", params, consumer)
    }

    /**
     * [getFlagList] RPC 返回 VM 中所有命令行标志及其当前值的列表。
     */
    fun getFlagList(consumer: FlagListConsumer) {
        val params = JsonObject()
        request("getFlagList", params, consumer)
    }

    /**
     * 返回指向由 [targetId] 指定对象的一组入站引用。最多返回 [limit] 个引用。
     */
    fun getInboundReferences(isolateId: String, targetId: String, limit: Int, consumer: GetInboundReferencesConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("targetId", targetId)
        params.addProperty("limit", limit)
        request("getInboundReferences", params, consumer)
    }

    /**
     * [getInstances] RPC 用于检索特定类的实例集合。
     * @param includeSubclasses 此参数是可选的，可以为 null。
     * @param includeImplementers 此参数是可选的，可以为 null。
     */
    fun getInstances(
        isolateId: String,
        objectId: String,
        limit: Int,
        includeSubclasses: Boolean?,
        includeImplementers: Boolean?,
        consumer: GetInstancesConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("objectId", objectId)
        params.addProperty("limit", limit)
        if (includeSubclasses != null) params.addProperty("includeSubclasses", includeSubclasses)
        if (includeImplementers != null) params.addProperty("includeImplementers", includeImplementers)
        request("getInstances", params, consumer)
    }

    /**
     * [getInstances] RPC 用于检索特定类的实例集合。
     */
    fun getInstances(isolateId: String, objectId: String, limit: Int, consumer: GetInstancesConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("objectId", objectId)
        params.addProperty("limit", limit)
        request("getInstances", params, consumer)
    }

    /**
     * [getInstancesAsList] RPC 用于检索特定类的实例集合。
     * 此 RPC 返回与包含所请求实例的 Dart <code>List<dynamic></code>List<dynamic> 对应的
     * <code>@Instance</code>@Instance。此 <code>List</code>List 不可增长，但其他方面是可变的。
     * 响应类型是区分此 RPC 与返回 <code>InstanceSet</code>InstanceSet 的 <code>getInstances</code>getInstances 的依据。
     * @param includeSubclasses 此参数是可选的，可以为 null。
     * @param includeImplementers 此参数是可选的，可以为 null。
     */
    fun getInstancesAsList(
        isolateId: String,
        objectId: String,
        includeSubclasses: Boolean?,
        includeImplementers: Boolean?,
        consumer: GetInstancesAsListConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("objectId", objectId)
        if (includeSubclasses != null) params.addProperty("includeSubclasses", includeSubclasses)
        if (includeImplementers != null) params.addProperty("includeImplementers", includeImplementers)
        request("getInstancesAsList", params, consumer)
    }

    /**
     * [getInstancesAsList] RPC 用于检索特定类的实例集合。
     * 此 RPC 返回与包含所请求实例的 Dart <code>List<dynamic></code>List<dynamic> 对应的
     * <code>@Instance</code>@Instance。此 <code>List</code>List 不可增长，但其他方面是可变的。
     * 响应类型是区分此 RPC 与返回 <code>InstanceSet</code>InstanceSet 的 <code>getInstances</code>getInstances 的依据。
     */
    fun getInstancesAsList(isolateId: String, objectId: String, consumer: GetInstancesAsListConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("objectId", objectId)
        request("getInstancesAsList", params, consumer)
    }

    /**
     * [getIsolate] RPC 用于通过其 [id] 查找 [Isolate] 对象。
     */
    fun getIsolate(isolateId: String, consumer: GetIsolateConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getIsolate", params, consumer)
    }

    /**
     * [getIsolateGroup] RPC 用于通过其 [id] 查找 [IsolateGroup] 对象。
     */
    fun getIsolateGroup(isolateGroupId: String, consumer: GetIsolateGroupConsumer) {
        val params = JsonObject()
        params.addProperty("isolateGroupId", isolateGroupId)
        request("getIsolateGroup", params, consumer)
    }

    /**
     * [getIsolateGroupMemoryUsage] RPC 用于通过其 [id] 查找隔离组的内存使用统计信息。
     */
    fun getIsolateGroupMemoryUsage(isolateGroupId: String, consumer: GetIsolateGroupMemoryUsageConsumer) {
        val params = JsonObject()
        params.addProperty("isolateGroupId", isolateGroupId)
        request("getIsolateGroupMemoryUsage", params, consumer)
    }

    /**
     * [getMemoryUsage] RPC 用于通过其 [id] 查找隔离区的内存使用统计信息。
     */
    fun getMemoryUsage(isolateId: String, consumer: GetMemoryUsageConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getMemoryUsage", params, consumer)
    }

    /**
     * [getObject] RPC 用于通过其 [id] 从某个隔离区查找 [object]。
     */
    override fun getObject(isolateId: String, objectId: String, consumer: GetObjectConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("objectId", objectId)
        request("getObject", params, consumer)
    }

    /**
     * [getObject] RPC 用于通过其 [id] 从某个隔离区查找 [object]。
     * @param offset 此参数是可选的，可以为 null。
     * @param count 此参数是可选的，可以为 null。
     */
    fun getObject(isolateId: String, objectId: String, offset: Int?, count: Int?, consumer: GetObjectConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("objectId", objectId)
        if (offset != null) params.addProperty("offset", offset)
        if (count != null) params.addProperty("count", count)
        request("getObject", params, consumer)
    }

    /**
     * [getPorts] RPC 用于检索给定隔离区的 <code>ReceivePort</code>ReceivePort 实例列表。
     */
    fun getPorts(isolateId: String, consumer: PortListConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getPorts", params, consumer)
    }

    /**
     * 返回 VM 已知的主要内存使用情况的描述。
     */
    fun getProcessMemoryUsage(consumer: ProcessMemoryUsageConsumer) {
        val params = JsonObject()
        request("getProcessMemoryUsage", params, consumer)
    }

    /**
     * [getRetainingPath] RPC 用于查找从由 [targetId] 指定的对象到 GC 根对象的路径
     * （即阻止此对象被垃圾回收的对象）。
     */
    fun getRetainingPath(isolateId: String, targetId: String, limit: Int, consumer: GetRetainingPathConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("targetId", targetId)
        params.addProperty("limit", limit)
        request("getRetainingPath", params, consumer)
    }

    /**
     * [getScripts] RPC 用于检索包含基于隔离区 [isolateId] 的所有脚本的 [ScriptList]。
     */
    fun getScripts(isolateId: String, consumer: GetScriptsConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getScripts", params, consumer)
    }

    /**
     * [getSourceReport] RPC 用于生成与隔离区中源位置关联的报告集。
     */
    fun getSourceReport(isolateId: String, reports: List<SourceReportKind>, consumer: GetSourceReportConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.add("reports", convertIterableToJsonArray(reports))
        request("getSourceReport", params, consumer)
    }

    /**
     * [getSourceReport] RPC 用于生成与隔离区中源位置关联的报告集。
     * @param scriptId 此参数是可选的，可以为 null。
     * @param tokenPos 此参数是可选的，可以为 null。
     * @param endTokenPos 此参数是可选的，可以为 null。
     * @param forceCompile 此参数是可选的，可以为 null。
     * @param reportLines 此参数是可选的，可以为 null。
     * @param libraryFilters 此参数是可选的，可以为 null。
     */
    fun getSourceReport(
        isolateId: String,
        reports: List<SourceReportKind>,
        scriptId: String?,
        tokenPos: Int?,
        endTokenPos: Int?,
        forceCompile: Boolean?,
        reportLines: Boolean?,
        libraryFilters: List<String>?,
        consumer: GetSourceReportConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.add("reports", convertIterableToJsonArray(reports))
        if (scriptId != null) params.addProperty("scriptId", scriptId)
        if (tokenPos != null) params.addProperty("tokenPos", tokenPos)
        if (endTokenPos != null) params.addProperty("endTokenPos", endTokenPos)
        if (forceCompile != null) params.addProperty("forceCompile", forceCompile)
        if (reportLines != null) params.addProperty("reportLines", reportLines)
        if (libraryFilters != null) params.add("libraryFilters", convertIterableToJsonArray(libraryFilters))
        request("getSourceReport", params, consumer)
    }

    /**
     * [getStack] RPC 用于检索隔离区的当前执行堆栈和消息队列。隔离区不需要暂停。
     */
    fun getStack(isolateId: String, consumer: GetStackConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("getStack", params, consumer)
    }

    /**
     * [getStack] RPC 用于检索隔离区的当前执行堆栈和消息队列。隔离区不需要暂停。
     * @param limit 此参数是可选的，可以为 null。
     */
    fun getStack(isolateId: String, limit: Int?, consumer: GetStackConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        if (limit != null) params.addProperty("limit", limit)
        request("getStack", params, consumer)
    }

    /**
     * [getSupportedProtocols] RPC 用于确定当前服务器支持的协议。
     */
    fun getSupportedProtocols(consumer: ProtocolListConsumer) {
        val params = JsonObject()
        request("getSupportedProtocols", params, consumer)
    }

    /**
     * [getVM] RPC 返回 Dart 虚拟机的全局信息。
     */
    fun getVM(consumer: VMConsumer) {
        val params = JsonObject()
        request("getVM", params, consumer)
    }

    /**
     * [getVMTimeline] RPC 用于检索包含 VM 时间线事件的对象。
     * @param timeOriginMicros 此参数是可选的，可以为 null。
     * @param timeExtentMicros 此参数是可选的，可以为 null。
     */
    fun getVMTimeline(timeOriginMicros: Int?, timeExtentMicros: Int?, consumer: TimelineConsumer) {
        val params = JsonObject()
        if (timeOriginMicros != null) params.addProperty("timeOriginMicros", timeOriginMicros)
        if (timeExtentMicros != null) params.addProperty("timeExtentMicros", timeExtentMicros)
        request("getVMTimeline", params, consumer)
    }

    /**
     * [getVMTimeline] RPC 用于检索包含 VM 时间线事件的对象。
     */
    fun getVMTimeline(consumer: TimelineConsumer) {
        val params = JsonObject()
        request("getVMTimeline", params, consumer)
    }

    /**
     * [getVMTimelineFlags] RPC 返回有关当前 VM 时间线配置的信息。
     */
    fun getVMTimelineFlags(consumer: TimelineFlagsConsumer) {
        val params = JsonObject()
        request("getVMTimelineFlags", params, consumer)
    }

    /**
     * [getVMTimelineMicros] RPC 返回时间线使用的时钟的当前时间戳，
     * 类似于 <code>dart:developer</code>dart:developer 中的
     * <code>Timeline.now</code>Timeline.now 和 VM 嵌入 API 中的
     * <code>Dart_TimelineGetMicros</code>Dart_TimelineGetMicros。
     */
    fun getVMTimelineMicros(consumer: TimestampConsumer) {
        val params = JsonObject()
        request("getVMTimelineMicros", params, consumer)
    }

    /**
     * [getVersion] RPC 用于确定 VM 提供的服务协议版本。
     */
    fun getVersion(consumer: VersionConsumer) {
        val params = JsonObject()
        request("getVersion", params, consumer)
    }

    /**
     * [invoke] RPC 用于对某个接收者执行常规方法调用，就像 dart:mirror 的 ObjectMirror.invoke 一样。
     * 注意，这不提供执行 getter、setter 或构造函数调用的方式。
     * @param disableBreakpoints 此参数是可选的，可以为 null。
     */
    fun invoke(
        isolateId: String,
        targetId: String,
        selector: String,
        argumentIds: List<String>,
        disableBreakpoints: Boolean?,
        consumer: InvokeConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("targetId", targetId)
        params.addProperty("selector", selector)
        params.add("argumentIds", convertIterableToJsonArray(argumentIds))
        if (disableBreakpoints != null) params.addProperty("disableBreakpoints", disableBreakpoints)
        request("invoke", params, consumer)
    }

    /**
     * [invoke] RPC 用于对某个接收者执行常规方法调用，就像 dart:mirror 的 ObjectMirror.invoke 一样。
     * 注意，这不提供执行 getter、setter 或构造函数调用的方式。
     */
    fun invoke(
        isolateId: String,
        targetId: String,
        selector: String,
        argumentIds: List<String>,
        consumer: InvokeConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("targetId", targetId)
        params.addProperty("selector", selector)
        params.add("argumentIds", convertIterableToJsonArray(argumentIds))
        request("invoke", params, consumer)
    }

    /**
     * [kill] RPC 用于终止隔离区，就像 dart:isolate 的
     * <code>Isolate.kill(IMMEDIATE)</code>Isolate.kill(IMMEDIATE) 一样。
     */
    fun kill(isolateId: String, consumer: KillConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("kill", params, consumer)
    }

    /**
     * [lookupPackageUris] RPC 用于将 URI 列表转换为未解析的路径。
     * 例如，传递给此 RPC 的 URI 按以下方式映射：
     */
    fun lookupPackageUris(isolateId: String, uris: List<String>, consumer: UriListConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.add("uris", convertIterableToJsonArray(uris))
        request("lookupPackageUris", params, consumer)
    }

    /**
     * [lookupResolvedPackageUris] RPC 用于将 URI 列表转换为已解析的（或绝对的）路径。
     * 例如，传递给此 RPC 的 URI 按以下方式映射：
     * @param local 此参数是可选的，可以为 null。
     */
    fun lookupResolvedPackageUris(isolateId: String, uris: List<String>, local: Boolean?, consumer: UriListConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.add("uris", convertIterableToJsonArray(uris))
        if (local != null) params.addProperty("local", local)
        request("lookupResolvedPackageUris", params, consumer)
    }

    /**
     * [lookupResolvedPackageUris] RPC 用于将 URI 列表转换为已解析的（或绝对的）路径。
     * 例如，传递给此 RPC 的 URI 按以下方式映射：
     */
    fun lookupResolvedPackageUris(isolateId: String, uris: List<String>, consumer: UriListConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.add("uris", convertIterableToJsonArray(uris))
        request("lookupResolvedPackageUris", params, consumer)
    }

    /**
     * [pause] RPC 用于中断正在运行的隔离区。
     * RPC 会将中断请求加入队列，并可能在隔离区暂停之前返回。
     */
    fun pause(isolateId: String, consumer: PauseConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("pause", params, consumer)
    }

    /**
     * 注册一个可由其他 VM 服务客户端调用的服务，
     * 其中 <code>service</code>service 是要通告的服务名称，
     * <code>alias</code>alias 是注册服务的替代名称。
     */
    fun registerService(service: String, alias: String, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.addProperty("service", service)
        params.addProperty("alias", alias)
        request("registerService", params, consumer)
    }

    /**
     * [reloadSources] RPC 用于对隔离区的源代码执行热重载。
     * @param force 此参数是可选的，可以为 null。
     * @param pause 此参数是可选的，可以为 null。
     * @param rootLibUri 此参数是可选的，可以为 null。
     * @param packagesUri 此参数是可选的，可以为 null。
     */
    fun reloadSources(
        isolateId: String,
        force: Boolean?,
        pause: Boolean?,
        rootLibUri: String?,
        packagesUri: String?,
        consumer: ReloadSourcesConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        if (force != null) params.addProperty("force", force)
        if (pause != null) params.addProperty("pause", pause)
        if (rootLibUri != null) params.addProperty("rootLibUri", rootLibUri)
        if (packagesUri != null) params.addProperty("packagesUri", packagesUri)
        request("reloadSources", params, consumer)
    }

    /**
     * [reloadSources] RPC 用于对隔离区的源代码执行热重载。
     */
    fun reloadSources(isolateId: String, consumer: ReloadSourcesConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("reloadSources", params, consumer)
    }

    /**
     * [removeBreakpoint] RPC 用于通过其 [id] 删除断点。
     */
    fun removeBreakpoint(isolateId: String, breakpointId: String, consumer: RemoveBreakpointConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("breakpointId", breakpointId)
        request("removeBreakpoint", params, consumer)
    }

    /**
     * 请求转储给定隔离区的 Dart 堆。
     */
    fun requestHeapSnapshot(isolateId: String, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("requestHeapSnapshot", params, consumer)
    }

    /**
     * [resume] RPC 用于恢复已暂停隔离区的执行。
     */
    fun resume(isolateId: String, consumer: ResumeConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("resume", params, consumer)
    }

    /**
     * [resume] RPC 用于恢复已暂停隔离区的执行。
     * @param step [StepOption] 指示在恢复 RPC 中请求的步进形式。
     *            此参数是可选的，可以为 null。
     * @param frameIndex 此参数是可选的，可以为 null。
     */
    fun resume(isolateId: String, step: StepOption?, frameIndex: Int?, consumer: ResumeConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        if (step != null) params.addProperty("step", step.name)
        if (frameIndex != null) params.addProperty("frameIndex", frameIndex)
        request("resume", params, consumer)
    }

    /**
     * [setBreakpointState] RPC 允许启用或禁用断点，而无需完全删除断点。
     */
    fun setBreakpointState(isolateId: String, breakpointId: String, enable: Boolean, consumer: BreakpointConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("breakpointId", breakpointId)
        params.addProperty("enable", enable)
        request("setBreakpointState", params, consumer)
    }

    /**
     * [setExceptionPauseMode] RPC 用于控制隔离区在抛出异常时是否暂停。
     * @param mode [ExceptionPauseMode] 指示隔离区在抛出异常时如何暂停。
     */
    @Deprecated("已弃用")
    fun setExceptionPauseMode(isolateId: String, mode: ExceptionPauseMode, consumer: SetExceptionPauseModeConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("mode", mode.name)
        request("setExceptionPauseMode", params, consumer)
    }

    /**
     * [setFlag] RPC 用于在运行时设置 VM 标志。
     * 如果指定的标志不存在、标志可能无法在运行时设置或值的类型与标志不匹配，则返回错误。
     */
    fun setFlag(name: String, value: String, consumer: SetFlagConsumer) {
        val params = JsonObject()
        params.addProperty("name", name)
        params.addProperty("value", value)
        request("setFlag", params, consumer)
    }

    /**
     * [setIsolatePauseMode] RPC 用于控制隔离区是否会因执行状态变化而暂停。
     * @param exceptionPauseMode [ExceptionPauseMode] 指示隔离区在抛出异常时如何暂停。
     *                          此参数是可选的，可以为 null。
     * @param shouldPauseOnExit 此参数是可选的，可以为 null。
     */
    fun setIsolatePauseMode(
        isolateId: String,
        exceptionPauseMode: ExceptionPauseMode?,
        shouldPauseOnExit: Boolean?,
        consumer: SetIsolatePauseModeConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        if (exceptionPauseMode != null) params.addProperty("exceptionPauseMode", exceptionPauseMode.name)
        if (shouldPauseOnExit != null) params.addProperty("shouldPauseOnExit", shouldPauseOnExit)
        request("setIsolatePauseMode", params, consumer)
    }

    /**
     * [setIsolatePauseMode] RPC 用于控制隔离区是否会因执行状态变化而暂停。
     */
    fun setIsolatePauseMode(isolateId: String, consumer: SetIsolatePauseModeConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request("setIsolatePauseMode", params, consumer)
    }

    /**
     * [setLibraryDebuggable] RPC 用于启用或禁用给定库的断点和步进功能。
     */
    fun setLibraryDebuggable(
        isolateId: String,
        libraryId: String,
        isDebuggable: Boolean,
        consumer: SetLibraryDebuggableConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("libraryId", libraryId)
        params.addProperty("isDebuggable", isDebuggable)
        request("setLibraryDebuggable", params, consumer)
    }

    /**
     * [setName] RPC 用于更改隔离区的调试名称。
     */
    fun setName(isolateId: String, name: String, consumer: SetNameConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("name", name)
        request("setName", params, consumer)
    }

    /**
     * [setTraceClassAllocation] RPC 允许启用或禁用特定类型对象的分配跟踪。
     * 可以使用 [getAllocationTraces] RPC 检索分配跟踪。
     */
    fun setTraceClassAllocation(
        isolateId: String,
        classId: String,
        enable: Boolean,
        consumer: SetTraceClassAllocationConsumer
    ) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        params.addProperty("classId", classId)
        params.addProperty("enable", enable)
        request("setTraceClassAllocation", params, consumer)
    }

    /**
     * [setVMName] RPC 用于更改 VM 的调试名称。
     */
    fun setVMName(name: String, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.addProperty("name", name)
        request("setVMName", params, consumer)
    }

    /**
     * [setVMTimelineFlags] RPC 用于设置启用的时间线流。
     */
    fun setVMTimelineFlags(recordedStreams: List<String>, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.add("recordedStreams", convertIterableToJsonArray(recordedStreams))
        request("setVMTimelineFlags", params, consumer)
    }

    /**
     * [streamCancel] RPC 用于取消 VM 中的流订阅。
     */
    fun streamCancel(streamId: String) {
        val params = JsonObject()
        params.addProperty("streamId", streamId)
        request("streamCancel", params, SuccessConsumer { })
    }

    /**
     * [streamCancel] RPC 用于取消 VM 中的流订阅。
     */
    fun streamCancel(streamId: String, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.addProperty("streamId", streamId)
        request("streamCancel", params, consumer)
    }

    /**
     * [streamCpuSamplesWithUserTag] RPC 允许客户端指定分析器收集的哪些 CPU 样本
     * 应该通过 <code>Profiler</code>Profiler 流发送。
     * 调用时，VM 将流式传输包含在 <code>userTags</code>userTags 中的用户标签处于活动状态时
     * 收集的 <code>CpuSample</code>CpuSample 的 <code>CpuSamples</code>CpuSamples 事件。
     */
    fun streamCpuSamplesWithUserTag(userTags: List<String>, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.add("userTags", convertIterableToJsonArray(userTags))
        request("streamCpuSamplesWithUserTag", params, consumer)
    }

    /**
     * [streamListen] RPC 用于订阅 VM 中的流。
     * 订阅后，客户端将开始接收来自流的事件。
     */
    fun streamListen(streamId: String, consumer: SuccessConsumer) {
        val params = JsonObject()
        params.addProperty("streamId", streamId)
        request("streamListen", params, consumer)
    }

    fun streamListen(streamId: String) {
        val params = JsonObject()
        params.addProperty("streamId", streamId)
        request("streamListen", params, SuccessConsumer {})
    }

    private fun convertIterableToJsonArray(list: Iterable<*>): JsonArray {
        val arr = JsonArray()
        for (element in list) {
            arr.add(JsonPrimitive(element.toString()))
        }
        return arr
    }

    private fun convertMapToJsonObject(map: Map<String, String>): JsonObject {
        val obj = JsonObject()
        for ((key, value) in map) {
            obj.addProperty(key, value)
        }
        return obj
    }

    override fun forwardResponse(consumer: Consumer, type: String, json: JsonObject) {
        when {
            consumer is AddBreakpointAtEntryConsumer -> {
                when (type) {
                    "Breakpoint" -> {
                        consumer.received(Breakpoint(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is AddBreakpointConsumer -> {
                when (type) {
                    "Breakpoint" -> {
                        consumer.received(Breakpoint(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is AddBreakpointWithScriptUriConsumer -> {
                when (type) {
                    "Breakpoint" -> {
                        consumer.received(Breakpoint(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is BreakpointConsumer -> {
                if (type == "Breakpoint") {
                    consumer.received(Breakpoint(json))
                    return
                }
            }

            consumer is ClearCpuSamplesConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is CpuSamplesConsumer -> {
                if (type == "CpuSamples") {
                    consumer.received(CpuSamples(json))
                    return
                }
            }

            consumer is EvaluateConsumer -> {
                when (type) {
                    "@Error" -> {
                        consumer.received(ErrorRef(json))
                        return
                    }

                    "@Instance" -> {
                        consumer.received(InstanceRef(json))
                        return
                    }

                    "@Null" -> {
                        consumer.received(NullRef(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is EvaluateInFrameConsumer -> {
                when (type) {
                    "@Error" -> {
                        consumer.received(ErrorRef(json))
                        return
                    }

                    "@Instance" -> {
                        consumer.received(InstanceRef(json))
                        return
                    }

                    "@Null" -> {
                        consumer.received(NullRef(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is FlagListConsumer -> {
                if (type == "FlagList") {
                    consumer.received(FlagList(json))
                    return
                }
            }

            consumer is GetAllocationProfileConsumer -> {
                when (type) {
                    "AllocationProfile" -> {
                        consumer.received(AllocationProfile(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetClassListConsumer -> {
                when (type) {
                    "ClassList" -> {
                        consumer.received(ClassList(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetCpuSamplesConsumer -> {
                when (type) {
                    "CpuSamples" -> {
                        consumer.received(CpuSamples(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetInboundReferencesConsumer -> {
                when (type) {
                    "InboundReferences" -> {
                        consumer.received(InboundReferences(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetInstancesAsListConsumer -> {
                when (type) {
                    "@Instance" -> {
                        consumer.received(InstanceRef(json))
                        return
                    }

                    "@Null" -> {
                        consumer.received(NullRef(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetInstancesConsumer -> {
                when (type) {
                    "InstanceSet" -> {
                        consumer.received(InstanceSet(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetIsolateConsumer -> {
                when (type) {
                    "Isolate" -> {
                        consumer.received(Isolate(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetIsolateGroupConsumer -> {
                when (type) {
                    "IsolateGroup" -> {
                        consumer.received(IsolateGroup(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetIsolateGroupMemoryUsageConsumer -> {
                when (type) {
                    "MemoryUsage" -> {
                        consumer.received(MemoryUsage(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetMemoryUsageConsumer -> {
                when (type) {
                    "MemoryUsage" -> {
                        consumer.received(MemoryUsage(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetObjectConsumer -> {
                when (type) {
                    "Breakpoint" -> {
                        consumer.received(Breakpoint(json))
                        return
                    }

                    "Class" -> {
                        consumer.received(ClassObj(json))
                        return
                    }

                    "Code" -> {
                        consumer.received(Code(json))
                        return
                    }

                    "Context" -> {
                        consumer.received(Context(json))
                        return
                    }

                    "Error" -> {
                        consumer.received(ErrorObj(json))
                        return
                    }

                    "Field" -> {
                        consumer.received(Field(json))
                        return
                    }

                    "Function" -> {
                        consumer.received(Func(json))
                        return
                    }

                    "Instance" -> {
                        consumer.received(Instance(json))
                        return
                    }

                    "Library" -> {
                        consumer.received(Library(json))
                        return
                    }

                    "Null" -> {
                        consumer.received(Null(json))
                        return
                    }

                    "Object" -> {
                        consumer.received(Obj(json))
                        return
                    }

                    "Script" -> {
                        consumer.received(Script(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "TypeArguments" -> {
                        consumer.received(TypeArguments(json))
                        return
                    }
                }
            }

            consumer is GetRetainingPathConsumer -> {
                when (type) {
                    "RetainingPath" -> {
                        consumer.received(RetainingPath(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetScriptsConsumer -> {
                when (type) {
                    "ScriptList" -> {
                        consumer.received(ScriptList(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is GetSourceReportConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "SourceReport" -> {
                        consumer.received(SourceReport(json))
                        return
                    }
                }
            }

            consumer is GetStackConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Stack" -> {
                        consumer.received(VmStack(json))
                        return
                    }
                }
            }

            consumer is InvokeConsumer -> {
                when (type) {
                    "@Error" -> {
                        consumer.received(ErrorRef(json))
                        return
                    }

                    "@Instance" -> {
                        consumer.received(InstanceRef(json))
                        return
                    }

                    "@Null" -> {
                        consumer.received(NullRef(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is KillConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is PauseConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is PortListConsumer -> {
                if (type == "PortList") {
                    consumer.received(PortList(json))
                    return
                }
            }

            consumer is ProcessMemoryUsageConsumer -> {
                if (type == "ProcessMemoryUsage") {
                    consumer.received(ProcessMemoryUsage(json))
                    return
                }
            }

            consumer is ProtocolListConsumer -> {
                if (type == "ProtocolList") {
                    consumer.received(ProtocolList(json))
                    return
                }
            }

            consumer is ReloadSourcesConsumer -> {
                when (type) {
                    "ReloadReport" -> {
                        consumer.received(ReloadReport(json))
                        return
                    }

                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }
                }
            }

            consumer is RemoveBreakpointConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is RequestHeapSnapshotConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is ResumeConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SetExceptionPauseModeConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SetFlagConsumer -> {
                when (type) {
                    "Error" -> {
                        consumer.received(ErrorObj(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SetIsolatePauseModeConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SetLibraryDebuggableConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SetNameConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SetTraceClassAllocationConsumer -> {
                when (type) {
                    "Sentinel" -> {
                        consumer.received(Sentinel(json))
                        return
                    }

                    "Success" -> {
                        consumer.received(Success(json))
                        return
                    }
                }
            }

            consumer is SuccessConsumer -> {
                if (type == "Success") {
                    consumer.received(Success(json))
                    return
                }
            }

            consumer is TimelineConsumer -> {
                if (type == "Timeline") {
                    consumer.received(Timeline(json))
                    return
                }
            }

            consumer is TimelineFlagsConsumer -> {
                if (type == "TimelineFlags") {
                    consumer.received(TimelineFlags(json))
                    return
                }
            }

            consumer is TimestampConsumer -> {
                if (type == "Timestamp") {
                    consumer.received(Timestamp(json))
                    return
                }
            }

            consumer is UriListConsumer -> {
                if (type == "UriList") {
                    consumer.received(UriList(json))
                    return
                }
            }

            consumer is VMConsumer -> {
                if (type == "VM") {
                    consumer.received(VM(json))
                    return
                }
            }

            consumer is VersionConsumer -> {
                if (type == "Version") {
                    consumer.received(Version(json))
                    return
                }
            }

            consumer is ServiceExtensionConsumer -> {
                consumer.received(json)
                return
            }
        }
        logUnknownResponse(consumer, json)
    }

    override fun equals(other: Any?): Boolean {
        if (other is VmService) {
            if (other.appId == this.appId) {
                return true
            }
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = gson.hashCode()
        result = 31 * result + appId.hashCode()
        result = 31 * result + appInfo.hashCode()
        return result
    }

    private suspend fun VmService.mainIsolates(): IsolateRef? {
        val vm = getVm()
        return vm.getIsolates().find { it.getName() == "main" }
    }

    private val isolateCache = ConcurrentHashMap<String, Isolate>()
    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    suspend fun getIsolateByIdPub(isolateId: String): Isolate? = getIsolateById(isolateId)

    suspend fun VmService.getIsolateById(isolateId: String): Isolate? {
        isolateCache[isolateId]?.let { return it }
        val mutex = mutexMap.getOrPut(isolateId) { Mutex() }
        mutex.withLock {
            isolateCache[isolateId]?.let { return it }
            return try {
                val isolate = fetchIsolateFromService(isolateId)
                isolate?.also { isolateCache[isolateId] = it }
                isolate
            } finally {
                mutexMap.remove(isolateId)
            }
        }
    }

    private suspend fun VmService.fetchIsolateFromService(isolateId: String): Isolate? {
        return suspendCancellableCoroutine { cont ->
            getIsolate(isolateId, object : GetIsolateConsumer {
                override fun received(response: Isolate) {
                    if (cont.isActive) {
                        cont.resume(response)
                    }
                }

                override fun received(response: Sentinel) {
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }

                override fun onError(error: RPCError) {
                    if (cont.isActive) {
                        cont.resumeWithException(error.exception)
                    }
                }
            })
        }
    }

}
