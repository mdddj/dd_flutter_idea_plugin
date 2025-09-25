package vm.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import shop.itbug.fluttercheckversionx.model.IRequest
import shop.itbug.fluttercheckversionx.model.formatDate
import vm.VmService
import vm.VmService.VmEventListener
import vm.element.Event
import vm.element.EventKind
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

// 网络监控管理器
class DartNetworkMonitor(
    private val vmService: VmService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
): VmEventListener {
    private val requests = mutableMapOf<String, NetworkRequest>()
    private val listeners = mutableListOf<NetworkRequestListener>()
    private var isMonitoring: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean>
        get() = isMonitoring
    private var lastUpdateTime = 0L
    private var pollingJob: Job? = null

    val taskJob get() = pollingJob

    private val isolateId get() = vmService.getMainIsolateId()

    init {
        vmService.addEventListener(this)
    }

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
        if (isMonitoring.value) return true

        return try {
            val isAvailable = vmService.isHttpProfilingAvailable(isolateId)
            if (!isAvailable) {
                notifyError("HTTP分析功能不可用")
                return false
            }
            val timelineResult = vmService.setHttpTimelineLogging(isolateId, true)
            if (timelineResult == null) {
                notifyError("启用HTTP时间线日志失败")
                return false
            }
            vmService.clearHttpProfile(isolateId)
            isMonitoring.value = true
            lastUpdateTime = System.currentTimeMillis() * 1000
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
        isMonitoring.value = false

        pollingJob?.let { job ->
            job.cancel()
            try {
                job.join() // 等待协程完全停止
            } catch (e: CancellationException) {
                throw e
            }
        }
        pollingJob = null

        try {
            vmService.setHttpTimelineLogging(isolateId, false)
        } catch (e: Exception) {
            println("停止监控时出错: ${e.message}")
        }
    }

    fun destroy() {
        scope.launch {
            stopMonitoring()
        }
        vmService.removeEventListener(this)
        requests.clear()
        listeners.clear()
    }

    private fun startPolling(intervalMs: Long = 1000L) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            try {
                while (isActive && isMonitoring.value) {
                    try {
                        updateRequests()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        notifyError("轮询网络数据失败: ${e.message}")
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

    suspend fun updateRequests() {
        if (!isMonitoring.value) return

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
        requests[existing.id] = existing
    }

    fun updateRequest(id: String, newRequest: NetworkRequest) {
        val request = requests[id]
        if(request!=null){
            requests[id] = newRequest
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
                    request.responseByteArray = bodyArray
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
        containsUrl: String? = null,
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

    override fun onVmEvent(streamId: String, event: Event) {
                when(event.getKind()){
                    EventKind.IsolateExit -> {
                        scope.launch {
                            stopMonitoring()
                        }
                    }
                    EventKind.IsolateStart -> {
                        scope.launch {
                            startMonitoring()
                        }
                    }
                    else -> {}
                }
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
                status = if (endTime != null) RequestStatus.COMPLETED else RequestStatus.PENDING,
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


