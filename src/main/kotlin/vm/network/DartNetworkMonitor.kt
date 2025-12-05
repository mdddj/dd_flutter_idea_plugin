package vm.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import shop.itbug.fluttercheckversionx.model.IRequest
import shop.itbug.fluttercheckversionx.model.formatDate
import vm.VmService
import vm.VmServiceComponent
import vm.getHttpProfile
import vm.isHttpProfilingAvailable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 监控到的网络请求数据模型
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
    @Transient
    var responseByteArray: JsonArray? = null,
    var contentLength: Long? = null,
    var error: String? = null,
    val events: MutableList<RequestEvent> = mutableListOf(),
    @Transient
    var networkMonitor: DartNetworkMonitor? = null,

    var isolateId: String
) : IRequest {

    val duration: Long?
        get() = endTime?.let { it - startTime }

    val isComplete: Boolean
        get() = status == RequestStatus.COMPLETED || status == RequestStatus.ERROR

    override val requestUrl: String get() = uri
    override val httpMethod: String get() = method
    override val httpStatusCode: Int get() = statusCode ?: -1
    override val durationMs: Long get() = duration ?: -1L
    override val httpRequestHeaders: Map<String, Any> get() = requestHeaders ?: emptyMap()
    override val httpResponseHeaders: Map<String, Any> get() = responseHeaders ?: emptyMap()
    override val httpRequestBody: Any? get() = requestBody
    override val httpResponseBody: Any? get() = responseBody

    override val requestStartTime: String
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()).formatDate()

    override val queryParams: Map<String, Any?>
        get() {
            return uri.substringAfter('?', "")
                .split('&')
                .filter { it.isNotEmpty() }
                .mapNotNull {
                    val parts = it.split('=', limit = 2)
                    if (parts.isNotEmpty()) parts[0] to parts.getOrNull(1) else null
                }
                .toMap()
        }
    val isImageResponse: Boolean
        get() {
            val contentType = responseHeaders?.entries
                ?.find { it.key.equals("Content-Type", ignoreCase = true) }
                ?.value
            return contentType?.startsWith("image/", ignoreCase = true) ?: false
        }

    val hasImageExtensionInUri: Boolean
        get() {
            val imageExtensions = setOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp")
            return imageExtensions.any { uri.substringBefore('?').endsWith(it, ignoreCase = true) }
        }


    /**
     * 综合判断是否为一个图片响应，具有更高的可靠性。
     * 优先检查 Content-Type，如果请求未完成或 header 缺失，则检查 URI 扩展名。
     */
    val isLikelyImage: Boolean
        get() = if (responseHeaders != null) isImageResponse else hasImageExtensionInUri
}

data class RequestEvent(
    val timestamp: Long,
    val event: String,
    var time1: String,
    val arguments: Map<String, Any>? = null,

    )

enum class RequestStatus {
    PENDING, COMPLETED, ERROR
}

interface TimeSource {
    fun currentTimeMicros(): Long
}

class SystemTimeSource : TimeSource {
    override fun currentTimeMicros() = System.currentTimeMillis() * 1000
}

// 网络监控管理器
class DartNetworkMonitor(
    private val vmService: VmService,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = SystemTimeSource(),
    private val maxRequestsCount: Int = 1000
) : VmServiceComponent {

    private val _requests = MutableStateFlow<Map<String, NetworkRequest>>(emptyMap())
    val requests: StateFlow<Map<String, NetworkRequest>> = _requests.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var lastUpdateTime = 0L
    private var pollingJob: Job? = null
    private var errorCount = 0
    private val maxConsecutiveErrors = 3
    private val logger = thisLogger()

    private val isolateId get() = vmService.getMainIsolateId()

    init {
        vmService.addEventHotResetListener(this)
        scope.launch {
            delay(100)
            startMonitoring()
        }
    }

    /**
     * 开始网络监控
     */
    suspend fun startMonitoring(intervalMs: Long = 1000L): Boolean {
        if (_isMonitoring.value) {
            logger.debug("监控已在运行中")
            return true
        }

        return try {
            val isAvailable = vmService.isHttpProfilingAvailable(isolateId)
            if (!isAvailable) {
                logger.warn("HTTP分析功能不可用")
                return false
            }

            val timelineResult = vmService.setHttpTimelineLogging(isolateId, true)
            if (timelineResult == null) {
                logger.warn("启用HTTP时间线日志失败")
                return false
            }

            vmService.clearHttpProfile(isolateId)
            _isMonitoring.value = true
            lastUpdateTime = timeSource.currentTimeMicros()
            errorCount = 0
            startPolling(intervalMs)

            logger.info("网络监控已启动")
            true
        } catch (e: Exception) {
            logger.error("启动网络监控失败", e)
            false
        }
    }

    fun stop() {
        scope.launch {
            stopMonitoring()
        }
    }

    /**
     * 停止网络监控
     */
    suspend fun stopMonitoring() {
        if (!_isMonitoring.value) return

        _isMonitoring.value = false

        pollingJob?.let { job ->
            job.cancel()
            try {
                job.join()
            } catch (e: CancellationException) {
                throw e
            }
        }
        pollingJob = null

        try {
            vmService.setHttpTimelineLogging(isolateId, false)
            logger.info("网络监控已停止")
        } catch (e: Exception) {
            logger.warn("停止监控时出错", e)
        }
    }


    /**
     * 启动轮询任务
     */
    private fun startPolling(intervalMs: Long) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            try {
                while (isActive && _isMonitoring.value) {
                    try {
                        updateRequests()
                        errorCount = 0 // 成功时重置错误计数
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        errorCount++
                        logger.error("轮询网络数据失败 (错误次数: $errorCount/$maxConsecutiveErrors)", e)

                        if (errorCount >= maxConsecutiveErrors) {
                            logger.error("连续错误过多,停止监控")
                            stopMonitoring()
                            break
                        }
                    }

                    delay(intervalMs)
                }
            } catch (e: CancellationException) {
                logger.debug("网络监控轮询已取消")
                throw e
            } catch (e: Exception) {
                logger.warn("轮询过程中发生严重错误", e)
            }
        }
    }

    suspend fun updateRequests() {
        if (!_isMonitoring.value) return

        try {
            // 注意：如果 lastUpdateTime 为 0，传 null 给 VM 比较好，获取全量
            val since = if (lastUpdateTime == 0L) null else lastUpdateTime

            val result = vmService.getHttpProfile(isolateId, since)

            result?.let { profileData ->
                parseHttpProfile(profileData)

                // --- 修复点 ---
                // 尝试从 VM 返回的数据中获取 timestamp，保证时间同步
                // 如果没有 timestamp，才回退到本地时间
                val vmTimestamp = profileData.get("timestamp")?.asLong
                lastUpdateTime = vmTimestamp ?: timeSource.currentTimeMicros()
            }
        } catch (e: CancellationException) {
            // 协程取消异常必须重新抛出，不能记录
            throw e
        } catch (e: Exception) {
            logger.warn("更新网络请求数据失败", e)
            // 不要抛出异常中断轮询，记录错误即可
            // throw e
        }
    }

    /**
     * 解析HTTP分析数据
     */
    private fun parseHttpProfile(profileData: JsonObject) {
        try {
            val requestsArray = profileData.getAsJsonArray("requests") ?: return

            requestsArray.forEach { element ->
                val requestJson = element.asJsonObject
                val request = parseNetworkRequest(requestJson, vmService)
                request.networkMonitor = this

                val existingRequest = _requests.value[request.id]
                if (existingRequest == null) {
                    // 新请求
                    addRequest(request)
                } else {
                    // 更新现有请求
                    val updatedRequest = mergeRequests(existingRequest, request)
                    updateRequest(request.id, updatedRequest)
                }
            }
        } catch (e: Exception) {
            logger.error("解析HTTP分析数据失败", e)
        }
    }

    /**
     * 添加新请求(带容量限制)
     */
    private fun addRequest(request: NetworkRequest) {
        _requests.value = _requests.value.let { current ->
            if (current.size >= maxRequestsCount) {
                // 移除最老的请求
                val sorted = current.values.sortedBy { it.startTime }
                val toRemove = sorted.take(current.size - maxRequestsCount + 1).map { it.id }
                (current - toRemove.toSet()) + (request.id to request)
            } else {
                current + (request.id to request)
            }
        }
    }

    private fun mergeRequests(existing: NetworkRequest, updated: NetworkRequest): NetworkRequest {
        // 合并事件列表
        val mergedEvents = mergeEvents(existing.events, updated.events)

        return existing.copy(
            // 只有当 updated 的 endTime 有值时才更新，否则保持原样（或者如果现有已经结束，就不应该变回未结束）
            endTime = updated.endTime ?: existing.endTime,

            // 状态合并逻辑：如果新状态是 COMPLETED/ERROR，则更新。如果新状态是 PENDING 但旧状态已经是 COMPLETED，则保持 COMPLETED
            status = if (existing.isComplete) existing.status else updated.status,

            // 关键点：使用 Elvis 操作符 (?:) 防止 valid 数据被 null 覆盖
            statusCode = updated.statusCode ?: existing.statusCode,
            requestHeaders = updated.requestHeaders ?: existing.requestHeaders,
            responseHeaders = updated.responseHeaders ?: existing.responseHeaders,
            requestBody = updated.requestBody ?: existing.requestBody,
            responseBody = updated.responseBody ?: existing.responseBody,
            contentLength = updated.contentLength ?: existing.contentLength,
            error = updated.error ?: existing.error,

            events = mergedEvents.toMutableList()
        )
    }

    /**
     * 合并事件列表,避免重复
     */
    private fun mergeEvents(existing: List<RequestEvent>, new: List<RequestEvent>): List<RequestEvent> {
        val existingSet = existing.map { it.timestamp to it.event }.toSet()
        return existing + new.filter { (it.timestamp to it.event) !in existingSet }
    }

    /**
     * 更新请求(线程安全)
     */
    fun updateRequest(id: String, newRequest: NetworkRequest) {
        _requests.value = _requests.value.let { current ->
            if (id in current) {
                current + (id to newRequest)
            } else {
                current
            }
        }
    }

    /**
     * 获取详细的请求信息(包括body)
     */
    suspend fun getRequestDetails(requestId: String): NetworkRequest? {
        return try {
            val result = vmService.getHttpProfileRequest(isolateId, requestId)
            result?.let { detailJson ->
                val request = _requests.value[requestId] ?: return null

                // 创建新的请求对象,保持不可变性
                val updatedRequest = request.copy(
                    requestBody = detailJson.getAsJsonArray("requestBody")?.let { decodeByteArray(it) },
                    responseBody = detailJson.getAsJsonArray("responseBody")?.let { decodeByteArray(it) },
                    responseByteArray = detailJson.getAsJsonArray("responseBody")
                )

                updateRequest(requestId, updatedRequest)
                updatedRequest
            }
        } catch (e: Exception) {
            logger.error("获取请求详细信息失败: requestId=$requestId", e)
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
            logger.warn("解码body失败", e)
            "Failed to decode body: ${e.message}"
        }
    }

    /**
     * 清除所有请求数据
     */
    suspend fun clearRequests() {
        _requests.value = emptyMap()
        try {
            vmService.clearHttpProfile(isolateId)
            lastUpdateTime = timeSource.currentTimeMicros()
            logger.info("请求数据已清除")
        } catch (e: Exception) {
            logger.error("清除请求数据失败", e)
        }
    }


    /**
     * 根据条件筛选请求
     */
    fun filterRequests(
        method: String? = null,
        status: RequestStatus? = null,
        containsUrl: String? = null,
    ): List<NetworkRequest> {
        return _requests.value.values.filter { request ->
            (method == null || request.method.equals(method, ignoreCase = true)) &&
                    (status == null || request.status == status) &&
                    (containsUrl == null || request.uri.contains(containsUrl, ignoreCase = true))
        }
    }


    override fun onExit() {
    }

    override fun onStart() {
        scope.launch {
            stopMonitoring()
            delay(100)
            startMonitoring()
        }
    }

    override fun dispose() {
        stop()
        _requests.value = emptyMap()
        vmService.removeEventHotResetListener(this)
    }

    companion object {
        /**
         * 解析单个网络请求
         */
        fun parseNetworkRequest(json: JsonObject, vmService: VmService): NetworkRequest {
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
                status = if (endTime != null) RequestStatus.COMPLETED else RequestStatus.PENDING,
                isolateId = vmService.getMainIsolateId()
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
                val timestamp = eventJson.get("timestamp")?.asLong ?: 0L
                val event = RequestEvent(
                    timestamp = timestamp,
                    event = eventJson.get("event")?.asString ?: "",
                    time1 = formatTime(timestamp),
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
                thisLogger().warn("解析请求数据失败", e)
            }
        }

        /**
         * 解析响应数据
         */
        private fun parseResponseData(request: NetworkRequest, responseJson: JsonObject) {
            // 1. 单独解析 statusCode，互不影响
            try {
                if (responseJson.has("statusCode")) {
                    request.statusCode = responseJson.get("statusCode").asInt
                }
            } catch (e: Exception) {
                // ignore
            }

            // 2. 单独解析 Headers，增加类型安全性
            try {
                responseJson.get("headers")?.asJsonObject?.let { headers ->
                    request.responseHeaders = headers.entrySet().associate { entry ->
                        val key = entry.key
                        val value = entry.value
                        // 兼容处理：如果是数组则 join，如果是字符串直接使用
                        val stringValue = if (value.isJsonArray) {
                            value.asJsonArray.joinToString(",") { it.asString }
                        } else {
                            value.asString
                        }
                        key to stringValue
                    }
                }
            } catch (e: Exception) {
                thisLogger().warn("解析响应头失败: ${e.message}")
            }

            // 3. 单独解析错误和结束时间
            try {
                responseJson.get("error")?.asString?.let { error ->
                    request.error = error
                    request.status = RequestStatus.ERROR
                }

                // 检查是否完成 (Response 里的 endTime 往往比外层的更准确用于判断是否传输结束)
                if (responseJson.has("endTime")) {
                    request.status = RequestStatus.COMPLETED
                }
            } catch (e: Exception) {
                thisLogger().warn("解析响应元数据失败", e)
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

        fun formatTime(dur: Long): String {
            return microsToFormattedTime(micros = dur)
        }

        fun microsToFormattedTime(micros: Long, pattern: String = "yyyy-MM-dd HH:mm:ss:SSS"): String {
            val millis = micros / 1000
            val instant = Instant.ofEpochMilli(millis)
            val formatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())
            return formatter.format(instant)
        }
    }
}