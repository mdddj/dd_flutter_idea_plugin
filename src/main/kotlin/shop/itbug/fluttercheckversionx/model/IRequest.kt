package shop.itbug.fluttercheckversionx.model

import com.google.gson.GsonBuilder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.config.DioCopyAllKey
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.toHexString
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.SwingUtilities


/**
 * 定义一个通用网络请求的抽象接口
 * 所有具体的请求模型都应该实现这个接口，以便重用通用功能。
 */
interface IRequest {
    /** 请求的完整 URL */
    val requestUrl: String

    /** HTTP 方法 (GET, POST, etc.) */
    val httpMethod: String?

    /** HTTP 状态码 */
    val httpStatusCode: Int

    /** 请求耗时 (毫秒) */
    val durationMs: Long

    /** 请求头 */
    val httpRequestHeaders: Map<String, Any>

    /** 响应头 */
    val httpResponseHeaders: Map<String, Any>

    /** 请求体 (可以是解析后的 Map/List，也可以是原始 String) */
    val httpRequestBody: Any?

    /** 响应体 (可以是解析后的 Map/List，也可以是原始 String) */
    val httpResponseBody: Any?

    /** 请求的查询参数 */
    val queryParams: Map<String, Any?>

    /** 请求开始时间 (格式化后的字符串) */
    val requestStartTime: String
}



/**
 * 计算响应体的大小
 */
fun IRequest.calculateSize(): String {
    try {
        if (this.httpResponseBody == null) {
            return "0 B"
        }
        return formatSize(httpResponseBody.toString().toByteArray().size.toLong())
    } catch (_: Exception) {
        return "0 B"
    }
}

/**
 * 判断请求是否成功
 */
fun IRequest.isSuccessful(): Boolean {
    return httpStatusCode in 200..299
}

private fun getInfoColor(): String = UIUtil.getLabelSuccessForeground().toHexString()
private fun getWarningColor(): String = UIUtil.getErrorForeground().toHexString()

/**
 * 生成用于UI展示的HTML前缀
 */
fun IRequest.getHtmlPrefix(): String {
    val color = if (isSuccessful()) getInfoColor() else getWarningColor()
    val secColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND.toHexString()
    fun getColorStyle(color: String) = "style='color:${color}'"

    return """
            <span ${getColorStyle(color)}>${httpStatusCode}</span> <span ${getColorStyle(secColor)}>${httpMethod}</span> <span ${getColorStyle(secColor)}>${durationMs}ms</span>
        """.trimIndent()
}

/**
 * 获取对应的消息类型，用于IDE通知
 */
fun IRequest.getMessageType(): MessageType {
    return if (isSuccessful()) MessageType.INFO else MessageType.ERROR
}

/**
 * 将请求数据转换为一个 Map，用于复制或序列化
 */
fun IRequest.getMap(config: DioCopyAllKey): Map<String, Any?> {
    val dataMap = mutableMapOf<String, Any?>(
        config.url to requestUrl,
        config.method to httpMethod,
        config.headers to httpRequestHeaders,
    )
    if (queryParams.isNotEmpty()) {
        dataMap[config.queryParams] = queryParams
    }
    httpRequestBody?.apply {
        dataMap[config.body] = this
    }
    dataMap[config.responseStatusCode] = httpStatusCode
    dataMap[config.response] = this.getDataJson() // 假设 getDataJson 也能适配 IRequest
    dataMap[config.requestTime] = requestStartTime
    dataMap[config.timestamp] = durationMs
    return dataMap
}

/**
 * 在编辑器中打开请求的JSON数据
 */
fun IRequest.openJsonDataInEditor(project: Project) {
    val config = DioListingUiConfig.setting.copyKeys
    val map = getMap(config)
    val toJson = DioApiService.getInstance().gson.toJson(map)
    val file = LightVirtualFile("request.json", toJson)
    SwingUtilities.invokeLater {
        FileEditorManager.getInstance(project).openFile(file)
    }
}

/**
 * 适配 getDataJson 函数，使其可以处理 IRequest
 */
fun IRequest.getDataJson(): Any? {
    return this.httpResponseBody
}


/**
 * Hurl 生成器，现在接收 IRequest
 */
class HurlGenerate(private val request: IRequest) {

    fun base(): String {
        return """
        |${request.httpMethod?.uppercase(Locale.getDefault())} ${request.requestUrl}
        |${getHeaders()}
        |${getQueryParams()}
        |${getPostBody()}
        |${httpSuccess()}
        """.trimMargin().trimIndent()
            .lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") + "\n"
    }

    private fun getHeaders(): String {
        return request.httpRequestHeaders
            .filter { it.key.lowercase() != "content-length" }
            .map { "${it.key}: ${it.value}" }
            .joinToString("\n")
    }

    private fun httpSuccess(): String {
        // 可以根据实际状态码生成
        return "HTTP ${request.httpStatusCode}"
    }

    private fun getQueryParams(): String {
        val queryParams = request.queryParams
        return if (queryParams.isNotEmpty()) {
            """
            |[Query]
            |${
                queryParams.filter { it.value != null && it.value.toString().isNotBlank() }
                    .map { "${it.key}: ${it.value}" }.joinToString("\n")
            }
            """.trimMargin().trimIndent()
        } else ""
    }

    private fun getPostBody(): String {
        return when (val body = request.httpRequestBody) {
            is String -> if(body.isNotBlank()) body else ""
            is Map<*, *> -> if (body.isNotEmpty()) GsonBuilder().setPrettyPrinting().create().toJson(body) else ""
            else -> body?.toString() ?: ""
        }
    }
}

// --- 辅助函数 (保持不变或稍作移动) ---
fun LocalDateTime.formatDate(): String {
    val f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    return this.format(f)
}

fun formatSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(sizeInBytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}