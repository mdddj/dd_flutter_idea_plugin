package vm.log


import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import vm.VmService
import vm.element.Event
import vm.element.InstanceRef
import vm.logging.FlutterEvent
import vm.retrieveFullStringValue
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.max

const val DEFAULT_LOG_BUFFER_REDUCTION_SIZE = 500

/**
 * DevTools 日志屏幕的核心控制器。
 * 负责从正在运行的应用中收集、管理、过滤和显示所有类型的日志。
 *
 * @param vmService VmService 实例，用于与 Dart VM 通信。
 * @param scope 一个与控制器生命周期绑定的 CoroutineScope，用于管理协程。
 */
@OptIn(FlowPreview::class)
class LoggingController(
    private val vmService: VmService,
    private val scope: CoroutineScope
) : VmService.VmEventListener {
    private val streamsToListen = listOf(
        VmService.STDOUT_STREAM_ID,
        VmService.STDERR_STREAM_ID,
        VmService.GC_STREAM_ID,
        VmService.LOGGING_STREAM_ID,
        VmService.EXTENSION_STREAM_ID
    )
    private val _allLogs = mutableListOf<LogData>()

    private val _filteredLogs = MutableStateFlow<List<LogData>>(emptyList())
    val filteredLogs: StateFlow<List<LogData>> = _filteredLogs.asStateFlow()

    private val _selectedLog = MutableStateFlow<LogData?>(null)
    val selectedLog: StateFlow<LogData?> = _selectedLog.asStateFlow()

    private val _statusText = MutableStateFlow("No logs")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    // --- 过滤条件 ---
    val searchQuery = MutableStateFlow("")
    val hideGcLogs = MutableStateFlow(true)
    // TODO: 可以根据需要添加更多过滤条件，比如 Flutter 框架日志等

    // --- 配置 ---
    var retentionLimit = 10000 // 日志保留上限

    private val stdoutHandler = StdoutEventHandler("stdout", isError = false)
    private val stderrHandler = StdoutEventHandler("stderr", isError = true)

    init {
        vmService.addEventListener(this)
        scope.launch {
            combine(searchQuery, hideGcLogs) { query, hideGc ->
                Pair(query, hideGc)
            }.debounce(150)
                .collect {
                    updateFilteredLogs()
                }
        }
        scope.launch {
            delay(100)
            startListenStream()
        }
    }


    private fun startListenStream() {
        streamsToListen.forEach(vmService::streamListen)
    }

    /**
     * 当 VmService 接收到事件时被调用。
     */
    override fun onVmEvent(streamId: String, event: Event) {
        scope.launch {
            when (streamId) {
                VmService.STDOUT_STREAM_ID -> stdoutHandler.handle(event)
                VmService.STDERR_STREAM_ID -> stderrHandler.handle(event)
                VmService.GC_STREAM_ID -> handleGCEvent(event)
                VmService.LOGGING_STREAM_ID -> handleDeveloperLogEvent(event)
                VmService.EXTENSION_STREAM_ID -> handleExtensionEvent(event)

            }
        }
    }

    private fun handleExtensionEvent(event: Event) {
        val kind = event.getExtensionKind()?.lowercase() ?: "extension"
        val timestamp = event.getTimestamp()
        val isolateRef = event.getIsolate()

        val summary = when (event.getExtensionKind()) {
            FlutterEvent.FRAME -> {
                val frameData = event.json.getAsJsonObject("extensionData")
                val number = frameData?.get("number")?.asInt ?: 0
                val elapsed = frameData?.get("elapsed")?.asDouble ?: 0.0
                "Frame #$number, ${String.format("%.1f", elapsed / 1000.0)}ms"
            }

            FlutterEvent.NAVIGATION -> {
                val routeData = event.json.getAsJsonObject("extensionData")?.getAsJsonObject("route")
                routeData?.get("description")?.asString ?: "Navigation event"
            }

            FlutterEvent.ERROR -> {
                val extensionData = event.json.getAsJsonObject("extensionData")
                findErrorSummary(extensionData) // 首选: 查找 "summary" 节点
                    ?: extensionData?.get("description")?.asString // 次选: 顶层描述
                    ?: extensionData?.get("renderedErrorText")?.asString?.lines()
                        ?.firstOrNull { it.isNotBlank() } // 备选: 格式化文本的第一行
                    ?: "Flutter Error" // 最终备选
            }

            else -> {
                // 其他Flutter事件
                event.getExtensionKind() ?: "Unknown Extension Event"
            }
        }

        val level = if (event.getExtensionKind() == FlutterEvent.ERROR) LogLevel.SEVERE else LogLevel.INFO
        val isError = event.getExtensionKind() == FlutterEvent.ERROR

        log(
            LogData(
                kind = kind,
                initialDetails = vmService.gson.toJson(event.json),
                timestamp = timestamp,
                summary = summary,
                level = level,
                isError = isError,
                isolateRef = isolateRef
            )
        )
    }

    private fun handleGCEvent(event: Event) {
        val reason = event.json.get("reason")?.asString ?: "unknown"
        val isolate = event.getIsolate()
        val summary = "${isolate?.getName() ?: "isolate"} • $reason collection"

        log(
            LogData(
                kind = "gc",
                initialDetails = vmService.gson.toJson(event.json),
                timestamp = event.getTimestamp(),
                summary = summary,
                isolateRef = isolate
            )
        )
    }

    private fun handleDeveloperLogEvent(event: Event) {
        val logRecord = event.json.getAsJsonObject("logRecord") ?: return
        val messageRefJson = logRecord.getAsJsonObject("message") ?: return
        val messageRef = InstanceRef(messageRefJson)
        val isolateRef = event.getIsolate()!!

        var summary = messageRef.getValueAsString()
        if (messageRef.getValueAsStringIsTruncated()) {
            summary += "..."
        }

        // 使用新的扩展函数来定义 detailsComputer
        val detailsComputer: (suspend () -> String)? = if (messageRef.getValueAsStringIsTruncated()) {
            {
                try {
                    vmService.retrieveFullStringValue(isolateRef.getId()!!, messageRef)
                        ?: "Full string value is null."
                } catch (e: Exception) {
                    "Error loading full string: ${e.message}"
                }
            }
        } else {
            null
        }

        log(
            LogData(
                kind = "log",
                initialDetails = summary,
                timestamp = event.getTimestamp(),
                summary = summary,
                level = logRecord.get("level")?.asInt ?: LogLevel.INFO,
                isolateRef = isolateRef,
                detailsComputer = detailsComputer
            )
        )
    }

    /**
     * 将一条新日志添加到控制器。
     * 这是所有日志来源的统一入口。
     */
    fun log(logData: LogData) {
        synchronized(_allLogs) {
            _allLogs.add(logData)
            updateForRetentionLimit()
        }
        updateFilteredLogs()
    }

    fun selectLog(log: LogData?) {
        _selectedLog.value = log
    }

    fun clear() {
        synchronized(_allLogs) {
            _allLogs.clear()
        }
        updateFilteredLogs()
    }

    fun dispose() {
        vmService.removeEventListener(this)
        streamsToListen.forEach(vmService::streamCancel)
        scope.cancel() // 取消所有协程
    }

    private fun updateFilteredLogs() {
        val query = searchQuery.value
        val hideGc = hideGcLogs.value

        val filtered = synchronized(_allLogs) {
            _allLogs.filter { log ->
                // 根据过滤条件进行过滤
                if (hideGc && log.kind == "gc") return@filter false

                if (query.isNotBlank()) {
                    val lowerQuery = query.lowercase()
                    (log.kind.lowercase().contains(lowerQuery) ||
                            log.summary?.lowercase()?.contains(lowerQuery) == true ||
                            log.details.value?.lowercase()?.contains(lowerQuery) == true)
                } else {
                    true
                }
            }
        }

        _filteredLogs.value = filtered
        updateStatusText()

        // 如果选中的日志被过滤掉了，则取消选择
        if (_selectedLog.value != null && !_filteredLogs.value.contains(_selectedLog.value)) {
            _selectedLog.value = null
        }
    }

    private fun updateForRetentionLimit() {
        // 保证此方法在 synchronized(_allLogs) 块内调用
        if (_allLogs.size > retentionLimit) {
            val reduceToSize = max(
                0,
                retentionLimit - DEFAULT_LOG_BUFFER_REDUCTION_SIZE
            )
            val removeCount = _allLogs.size - reduceToSize
            if (removeCount > 0) {
                _allLogs.subList(0, removeCount).clear()
            }
        }
    }

    private fun updateStatusText() {
        val totalCount = synchronized(_allLogs) { _allLogs.size }
        val showingCount = _filteredLogs.value.size

        _statusText.value = if (totalCount == showingCount) {
            "$totalCount events"
        } else {
            "Showing $showingCount of $totalCount events"
        }
    }

    /**
     * 内部类，用于处理 stdout/stderr 事件，并合并换行符。
     */
    private inner class StdoutEventHandler(
        private val name: String,
        private val isError: Boolean
    ) {
        private var buffer: LogData? = null
        private var timerJob: Job? = null

        fun handle(e: Event) {
            val message = try {
                val bytes = Base64.getDecoder().decode(e.getBytes())
                String(bytes, StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                return // 解码失败则忽略
            }

            timerJob?.cancel()

            val currentBuffer = buffer
            if (currentBuffer != null) {
                if (message == "\n") {
                    // 合并换行符
                    val mergedLog = currentBuffer.copy(
                        initialDetails = currentBuffer.details.value + message,
                        summary = currentBuffer.summary + message
                    )
                    this.buffer = null
                    log(mergedLog)
                    return
                } else {
                    // 前一个buffer未等到换行符，直接输出
                    this.buffer = null
                    log(currentBuffer)
                }
            }

            val newLog = LogData(
                kind = name,
                initialDetails = message,
                timestamp = e.getTimestamp(),
                summary = message.take(200), // 截取摘要
                isError = isError,
                level = if (isError) LogLevel.SEVERE else LogLevel.INFO,
                isolateRef = e.getIsolate()
            )

            if (message != "\n") {
                this.buffer = newLog
                timerJob = scope.launch {
                    delay(5) // 短暂延迟等待换行符
                    if (isActive && this@StdoutEventHandler.buffer != null) {
                        log(this@StdoutEventHandler.buffer!!)
                        this@StdoutEventHandler.buffer = null
                    }
                }
            } else {
                log(newLog)
            }
        }
    }
}


/**
 * 递归地在 Flutter 错误诊断树中查找摘要信息。
 * @param node 当前要搜索的 JsonObject 节点。
 * @return 摘要字符串，如果未找到则返回 null。
 */
private fun findErrorSummary(node: JsonObject?): String? {
    if (node == null) return null

    // 策略1: 检查当前节点是否为摘要节点
    if (node.has("level") && node.get("level").asString == "summary") {
        return node.get("description")?.asString
    }

    // 递归搜索 'properties' 数组
    if (node.has("properties")) {
        node.getAsJsonArray("properties")?.forEach { element ->
            val summary = findErrorSummary(element.asJsonObjectOrNull())
            if (summary != null) return summary
        }
    }

    // 递归搜索 'children' 数组
    if (node.has("children")) {
        node.getAsJsonArray("children")?.forEach { element ->
            val summary = findErrorSummary(element.asJsonObjectOrNull())
            if (summary != null) return summary
        }
    }

    return null
}

// 辅助扩展函数，用于安全地转换 JsonElement
private fun JsonElement.asJsonObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null