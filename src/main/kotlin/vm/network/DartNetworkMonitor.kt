package vm.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import shop.itbug.fluttercheckversionx.model.IRequest
import shop.itbug.fluttercheckversionx.model.formatDate
import vm.VmService
import vm.getHttpProfile
import vm.isHttpProfilingAvailable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 监控统计数据
 */
data class MonitoringStats(
    val totalRequests: Int,
    val completedRequests: Int,
    val errorRequests: Int,
    val pendingRequests: Int,
    val averageResponseTime: Double
)

/**
 * 监控到的网络请求数据模型 (已实现 IRequest 接口)
 */
data class NetworkRequest(
    val id: String,
    val method: String,
    val uri: String,
    val startTime: Long,
    var endTime: Long? = null,
    var status: RequestStatus = RequestStatus.PENDING,
    var statusCode: Int? = null,
    var requestHeaders: Map<String, String>? = null,
    var responseHeaders: Map<String, String>? = null,
    var requestBody: String? = null,
    var responseBody: String? = null,
    var contentLength: Long? = null,
    var error: String? = null,
    val events: MutableList<RequestEvent> = mutableListOf(),
    var networkMonitor: DartNetworkMonitor? = null
) : IRequest { // <--- 实现接口

    val duration: Long?
        get() = endTime?.let { it - startTime }

    val isComplete: Boolean
        get() = status == RequestStatus.COMPLETED || status == RequestStatus.ERROR

    // --- 实现 IRequest 接口的属性 ---
    override val requestUrl: String get() = uri
    override val httpMethod: String? get() = method
    override val httpStatusCode: Int get() = statusCode ?: -1 // 如果为 null，提供一个默认值
    override val durationMs: Long get() = duration ?: -1L // 如果为 null，提供一个默认值
    override val httpRequestHeaders: Map<String, Any> get() = requestHeaders ?: emptyMap()
    override val httpResponseHeaders: Map<String, Any> get() = responseHeaders ?: emptyMap()
    override val httpRequestBody: Any? get() = requestBody
    override val httpResponseBody: Any? get() = responseBody

    // 从 startTime (Long) 转换
    override val requestStartTime: String
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()).formatDate()

    override val queryParams: Map<String, Any?>
        get() {
            // 如果 URI 包含查询参数，需要解析它们。这是一个简单的实现。
            return uri.substringAfter('?', "")
                .split('&')
                .filter { it.isNotEmpty() }
                .mapNotNull {
                    val parts = it.split('=', limit = 2)
                    if (parts.isNotEmpty()) parts[0] to parts.getOrNull(1) else null
                }
                .toMap()
        }

    // ... companion object 和其他代码保持不变 ...
}

data class RequestEvent(
    val timestamp: Long,
    val event: String,
    val arguments: Map<String, Any>? = null
)

enum class RequestStatus {
    PENDING, COMPLETED, ERROR
}

// 网络监控管理器
class DartNetworkMonitor(
    private val vmService: VmService,
    private val isolateId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val requests = mutableMapOf<String, NetworkRequest>()
    private val listeners = mutableListOf<NetworkRequestListener>()
    var isMonitoring = false
    private var lastUpdateTime = 0L
    private var pollingJob: Job? = null

    val taskJob get() = pollingJob

    interface NetworkRequestListener {
        fun onRequestStarted(request: NetworkRequest)
        fun onRequestUpdated(request: NetworkRequest)
        fun onRequestCompleted(request: NetworkRequest)
        fun onError(error: String)
    }

    open class DefaultNetworkRequestListener(
        val started: (req: NetworkRequest) -> Unit,
        val update: (req: NetworkRequest) -> Unit,
        val completed: (req: NetworkRequest) -> Unit,
        val onError: (err: String) -> Unit = {}
    ) : NetworkRequestListener {
        override fun onRequestStarted(request: NetworkRequest) = started.invoke(request)
        override fun onRequestUpdated(request: NetworkRequest) = update.invoke(request)
        override fun onRequestCompleted(request: NetworkRequest) = completed.invoke(request)
        override fun onError(error: String) = onError.invoke(error)

    }

    /**
     * 开始网络监控
     */
    suspend fun startMonitoring(intervalMs: Long = 1000L): Boolean {
        if (isMonitoring) return true

        return try {
            // 1. 检查HTTP分析可用性
            val isAvailable = vmService.isHttpProfilingAvailable(isolateId)
            if (!isAvailable) {
                notifyError("HTTP分析功能不可用")
                return false
            }

            // 2. 启用HTTP时间线日志
            val timelineResult = vmService.setHttpTimelineLogging(isolateId, true)
            if (timelineResult == null) {
                notifyError("启用HTTP时间线日志失败")
                return false
            }

            // 3. 清除旧数据
            vmService.clearHttpProfile(isolateId)

            isMonitoring = true
            lastUpdateTime = System.currentTimeMillis() * 1000 // 转换为微秒

            // 4. 开始定期轮询
            startPolling(intervalMs)

            true
        } catch (e: Exception) {
            notifyError("启动网络监控失败: ${e.message}")
            false
        }
    }

    /**
     * 停止网络监控
     */
    suspend fun stopMonitoring() {
        isMonitoring = false

        // 取消轮询任务
        pollingJob?.let { job ->
            job.cancel()
            try {
                job.join() // 等待协程完全停止
            } catch (e: CancellationException) {
                // 正常的取消，忽略
                throw e
            }
        }
        pollingJob = null

        try {
            // 禁用HTTP时间线日志
            vmService.setHttpTimelineLogging(isolateId, false)
        } catch (e: Exception) {
            println("停止监控时出错: ${e.message}")
        }
    }

    /**
     * 销毁监控器，释放所有资源
     */
    fun destroy() {
        scope.launch {
            stopMonitoring()
        }

        // 清理数据
        requests.clear()
        listeners.clear()
    }

    /**
     * 开始轮询HTTP分析数据
     */
    private fun startPolling(intervalMs: Long = 1000L) {
        // 取消现有的轮询任务
        pollingJob?.cancel()

        pollingJob = scope.launch {
            try {
                while (isActive && isMonitoring) {
                    try {
                        updateRequests()
                    } catch (e: CancellationException) {
                        // 正常的协程取消，不需要处理
                        throw e
                    } catch (e: Exception) {
                        notifyError("轮询网络数据失败: ${e.message}")
                        // 出现错误时稍微延长等待时间
                        delay(intervalMs * 2)
                        continue
                    }

                    // 等待指定的轮询间隔
                    delay(intervalMs)
                }
            } catch (e: CancellationException) {
                println("网络监控轮询已停止")
                throw e
            } catch (e: Exception) {
                notifyError("轮询过程中发生严重错误: ${e.message}")
            }
        }
    }

    /**
     * 获取HTTP分析数据并更新请求列表
     */
    suspend fun updateRequests() {
        if (!isMonitoring) return

        try {
            val result = vmService.getHttpProfile(isolateId, lastUpdateTime)
            result?.let { profileData ->
                parseHttpProfile(profileData)
                lastUpdateTime = System.currentTimeMillis() * 1000
            }
        } catch (e: Exception) {
            notifyError("更新网络请求数据失败: ${e.message}")
        }
    }

    /**
     * 解析HTTP分析数据
     */
    private fun parseHttpProfile(profileData: JsonObject) {
        try {
            val timestamp = profileData.get("timestamp")?.asLong ?: return
            val requestsArray = profileData.getAsJsonArray("requests") ?: return

            requestsArray.forEach { element ->
                val requestJson = element.asJsonObject
                val request = parseNetworkRequest(requestJson)
                request.networkMonitor = this

                val existingRequest = requests[request.id]?.copy()
                if (existingRequest == null) {
                    // 新请求
                    requests[request.id] = request
                    notifyRequestStarted(request)
                } else {
                    // 更新现有请求
                    updateExistingRequest(existingRequest, request)
                    notifyRequestUpdated(existingRequest)

                    if (existingRequest.isComplete) {
                        notifyRequestCompleted(existingRequest)
                    }
                }
            }
        } catch (e: Exception) {
            notifyError("解析HTTP分析数据失败: ${e.message}")
        }
    }


    /**
     * 更新现有请求
     */
    private fun updateExistingRequest(existing: NetworkRequest, updated: NetworkRequest) {
        existing.endTime = updated.endTime
        existing.status = updated.status
        existing.statusCode = updated.statusCode
        existing.responseHeaders = updated.responseHeaders
        existing.error = updated.error

        // 合并事件
        updated.events.forEach { newEvent ->
            if (!existing.events.any { it.timestamp == newEvent.timestamp && it.event == newEvent.event }) {
                existing.events.add(newEvent)
            }
        }
    }

    /**
     * 获取详细的请求信息（包括body）
     */
    suspend fun getRequestDetails(requestId: String): NetworkRequest? {
        return try {
            val result = vmService.getHttpProfileRequest(isolateId, requestId)
            result?.let { detailJson ->
                val request = requests[requestId] ?: return null

                // 解析请求body
                detailJson.getAsJsonArray("requestBody")?.let { bodyArray ->
                    request.requestBody = decodeByteArray(bodyArray)
                }

                // 解析响应body
                detailJson.getAsJsonArray("responseBody")?.let { bodyArray ->
                    request.responseBody = decodeByteArray(bodyArray)
                }

                request
            }
        } catch (e: Exception) {
            println("获取请求详细信息失败: ${e.message}")
            null
        }
    }

    /**
     * 解码字节数组为字符串
     */
    private fun decodeByteArray(jsonArray: JsonArray): String {
        return try {
            val bytes = jsonArray.map { it.asInt.toByte() }.toByteArray()
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Failed to decode body: ${e.message}"
        }
    }

    /**
     * 清除所有请求数据
     */
    suspend fun clearRequests() {
        requests.clear()
        try {
            vmService.clearHttpProfile(isolateId)
            lastUpdateTime = System.currentTimeMillis() * 1000
        } catch (e: Exception) {
            notifyError("清除请求数据失败: ${e.message}")
        }
    }

    /**
     * 获取所有请求
     */
    fun getAllRequests(): List<NetworkRequest> = requests.values.toList()

    /**
     * 根据条件筛选请求
     */
    fun filterRequests(
        method: String? = null,
        status: RequestStatus? = null,
        containsUrl: String? = null
    ): List<NetworkRequest> {
        return requests.values.filter { request ->
            (method == null || request.method.equals(method, ignoreCase = true)) &&
                    (status == null || request.status == status) &&
                    (containsUrl == null || request.uri.contains(containsUrl, ignoreCase = true))
        }
    }

    /**
     * 添加监听器
     */
    fun addListener(listener: NetworkRequestListener) {
        listeners.add(listener)
    }

    /**
     * 移除监听器
     */
    fun removeListener(listener: NetworkRequestListener) {
        listeners.remove(listener)
    }

    // 通知方法
    private fun notifyRequestStarted(request: NetworkRequest) {
        listeners.forEach { it.onRequestStarted(request) }
    }

    private fun notifyRequestUpdated(request: NetworkRequest) {
        listeners.forEach { it.onRequestUpdated(request) }
    }

    private fun notifyRequestCompleted(request: NetworkRequest) {
        listeners.forEach { it.onRequestCompleted(request) }
    }

    private fun notifyError(error: String) {
        listeners.forEach { it.onError(error) }
    }

    companion object {
        /**
         * 解析单个网络请求
         */
        fun parseNetworkRequest(json: JsonObject): NetworkRequest {
            val id = json.get("id")?.asString ?: ""
            val method = json.get("method")?.asString ?: ""
            val uri = json.get("uri")?.asString ?: ""
            val startTime = json.get("startTime")?.asLong ?: 0L
            val endTime = json.get("endTime")?.asLong

            val request = NetworkRequest(
                id = id,
                method = method,
                uri = uri,
                startTime = startTime,
                endTime = endTime,
                status = if (endTime != null) RequestStatus.COMPLETED else RequestStatus.PENDING
            )

            // 解析请求数据
            json.get("request")?.asJsonObject?.let { requestData ->
                parseRequestData(request, requestData)
            }

            // 解析响应数据
            json.get("response")?.asJsonObject?.let { responseData ->
                parseResponseData(request, responseData)
            }

            // 解析事件
            json.getAsJsonArray("events")?.forEach { eventElement ->
                val eventJson = eventElement.asJsonObject
                val event = RequestEvent(
                    timestamp = eventJson.get("timestamp")?.asLong ?: 0L,
                    event = eventJson.get("event")?.asString ?: "",
                    arguments = parseEventArguments(eventJson.get("arguments")?.asJsonObject)
                )
                request.events.add(event)
            }

            return request
        }

        /**
         * 解析请求数据
         */
        private fun parseRequestData(request: NetworkRequest, requestJson: JsonObject) {
            try {
                // 解析请求头
                requestJson.get("headers")?.asJsonObject?.let { headers ->
                    request.requestHeaders = headers.entrySet().associate {
                        it.key to it.value.asString
                    }
                }

                // 解析内容长度
                request.contentLength = requestJson.get("contentLength")?.asLong

                // 检查错误
                requestJson.get("error")?.asString?.let { error ->
                    request.error = error
                    request.status = RequestStatus.ERROR
                }
            } catch (e: Exception) {
                println("解析请求数据失败: ${e.message}")
            }
        }

        /**
         * 解析响应数据
         */
        private fun parseResponseData(request: NetworkRequest, responseJson: JsonObject) {
            try {
                // 状态码
                request.statusCode = responseJson.get("statusCode")?.asInt

                // 响应头
                responseJson.get("headers")?.asJsonObject?.let { headers ->
                    request.responseHeaders = headers.entrySet().associate {
                        it.key to it.value.asJsonArray.joinToString(",") { it1 -> it1.asString }
                    }
                }

                // 检查错误
                responseJson.get("error")?.asString?.let { error ->
                    request.error = error
                    request.status = RequestStatus.ERROR
                }

                // 检查是否完成
                responseJson.get("endTime")?.let {
                    request.status = RequestStatus.COMPLETED
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("解析响应数据失败: ${e.message}")
            }
        }

        /**
         * 解析事件参数
         */
        private fun parseEventArguments(argumentsJson: JsonObject?): Map<String, Any>? {
            return argumentsJson?.entrySet()?.associate { entry ->
                entry.key to when {
                    entry.value.isJsonPrimitive -> {
                        val primitive = entry.value.asJsonPrimitive
                        when {
                            primitive.isString -> primitive.asString
                            primitive.isNumber -> primitive.asNumber
                            primitive.isBoolean -> primitive.asBoolean
                            else -> primitive.asString
                        }
                    }

                    else -> entry.value.toString()
                }
            }
        }
    }
}

// 使用示例
class NetworkMonitorExample {
    private var networkMonitor: DartNetworkMonitor? = null
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun setupNetworkMonitoring(vmService: VmService) {
        val isolateId = vmService.getMainIsolateId()
        if (isolateId.isBlank()) return

        // 创建监控器，使用专门的协程作用域
        networkMonitor = DartNetworkMonitor(vmService, isolateId, monitorScope)

        // 添加监听器
        networkMonitor?.addListener(object : DartNetworkMonitor.NetworkRequestListener {
            override fun onRequestStarted(request: NetworkRequest) {
                println("新请求: ${request.method} ${request.uri}")
            }

            override fun onRequestUpdated(request: NetworkRequest) {
                println("请求更新: ${request.id} - ${request.status}")
            }

            override fun onRequestCompleted(request: NetworkRequest) {
                println("请求完成: ${request.method} ${request.uri} - ${request.statusCode} (${request.duration}ms)")
            }

            override fun onError(error: String) {
                println("监控错误: $error")
            }
        })

        // 开始监控
        val success = networkMonitor?.startMonitoring() ?: false
        if (success) {
            println("网络监控已启动")
        } else {
            println("启动网络监控失败")
        }
    }

    /**
     * 手动更新请求数据（可选，因为已经有自动轮询）
     */
    fun manualUpdate() {
        monitorScope.launch {
            try {
                networkMonitor?.updateRequests()
            } catch (e: Exception) {
                println("手动更新失败: ${e.message}")
            }
        }
    }

    /**
     * 获取实时请求数据流
     */
    fun getRequestFlow(): Flow<List<NetworkRequest>> = flow {
        while (currentCoroutineContext().isActive) {
            val requests = networkMonitor?.getAllRequests() ?: emptyList()
            emit(requests)
            delay(500) // 每500ms发送一次更新
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getRequestDetails(requestId: String) {
        val details = networkMonitor?.getRequestDetails(requestId)
        details?.let { request ->
            println("请求详情:")
            println("URL: ${request.uri}")
            println("方法: ${request.method}")
            println("状态码: ${request.statusCode}")
            println("请求头: ${request.requestHeaders}")
            println("响应头: ${request.responseHeaders}")
            println("请求体: ${request.requestBody}")
            println("响应体: ${request.responseBody}")
        }
    }

    /**
     * 停止监控并清理资源
     */
    suspend fun stopMonitoring() {
        networkMonitor?.stopMonitoring()
        networkMonitor?.destroy()
        networkMonitor = null

        // 取消所有相关协程
        monitorScope.cancel()
    }

    /**
     * 暂停/恢复监控（保持数据但停止轮询）
     */
    suspend fun pauseMonitoring() {
        networkMonitor?.stopMonitoring()
    }

    suspend fun resumeMonitoring() {
        networkMonitor?.startMonitoring()
    }

    /**
     * 获取监控统计信息
     */
    fun getMonitoringStats(): MonitoringStats {
        val requests = networkMonitor?.getAllRequests() ?: emptyList()
        return MonitoringStats(
            totalRequests = requests.size,
            completedRequests = requests.count { it.status == RequestStatus.COMPLETED },
            errorRequests = requests.count { it.status == RequestStatus.ERROR },
            pendingRequests = requests.count { it.status == RequestStatus.PENDING },
            averageResponseTime = requests.mapNotNull { it.duration }.average().takeIf { !it.isNaN() } ?: 0.0
        )
    }
}