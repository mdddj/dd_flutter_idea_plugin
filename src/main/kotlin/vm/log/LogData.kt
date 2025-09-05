package vm.log

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vm.element.IsolateRef
import java.text.SimpleDateFormat
import java.util.*

object LogLevel {
    const val ALL = 0
    const val INFO = 800
    const val SEVERE = 1000

    fun getName(level: Int): String {
        return when {
            level >= SEVERE -> "SEVERE"
            level >= INFO -> "INFO"
            else -> "ALL"
        }
    }
}

/**
 * 日志数据的数据模型，代表日志列表中的一行。
 *
 * @param kind 日志类型 (e.g., "stdout", "gc", "flutter.frame")
 * @param initialDetails 初始的日志详情，可能是截断的
 * @param timestamp 事件发生的时间戳 (毫秒)
 * @param summary 日志的简短摘要，用于在列表中显示
 * @param level 日志级别，用于着色和过滤
 * @param isError 是否为错误日志
 * @param isolateRef 相关的 Isolate 引用
 * @param detailsComputer 一个挂起函数，用于懒加载完整的日志详情
 */
data class LogData(
    val kind: String,
    private val initialDetails: String?,
    val timestamp: Long,
    val summary: String?,
    val level: Int = LogLevel.INFO,
    val isError: Boolean = false,
    val isolateRef: IsolateRef?,
    val detailsComputer: (suspend () -> String)? = null
) {
    private val _details = MutableStateFlow(initialDetails)
    val details: StateFlow<String?> = _details.asStateFlow()

    private var detailsComputationJob: Job? = null
    val isComputingDetails: Boolean get() = detailsComputationJob?.isActive == true
    val needsComputing: Boolean get() = detailsComputer != null && detailsComputationJob == null

    val levelName: String by lazy { LogLevel.getName(level) }
    val formattedTimestamp: String by lazy {
        timeFormat.format(Date(timestamp))
    }

    /**
     * 如果需要，异步计算并获取完整的日志详情。
     * 这个方法是幂等的，只会执行一次计算。
     * @param scope 用于启动计算协程的 CoroutineScope
     */
    fun computeDetails(scope: CoroutineScope) {
        if (needsComputing) {
            detailsComputationJob = scope.launch {
                try {
                    _details.value = detailsComputer?.invoke()
                } catch (e: Exception) {
                    _details.value = "Failed to load details: ${e.message}"
                }
            }
        }
    }

    /**
     * 返回格式化后的 JSON 详情，如果详情不是合法的 JSON，则返回原始文本。
     */
    fun prettyPrinted(): String? {
        val currentDetails = details.value ?: return null
        return try {
            val jsonElement = JsonParser.parseString(currentDetails)
            prettyJsonEncoder.toJson(jsonElement)
        } catch (_: Exception) {
            currentDetails.trim()
        }
    }


    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        private val prettyJsonEncoder = GsonBuilder().setPrettyPrinting().create()
    }
}