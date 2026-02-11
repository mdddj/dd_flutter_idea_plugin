package shop.itbug.flutterx.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import shop.itbug.flutterx.config.DioCopyAllKey
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.socket.SocketResponseModel
import shop.itbug.flutterx.socket.service.DioApiService
import shop.itbug.flutterx.util.toHexString
import vm.network.NetworkRequest
import java.net.URLEncoder
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.SwingUtilities
import kotlin.math.log10
import kotlin.math.pow


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


fun IRequest.isDioRequest(): Boolean = this is SocketResponseModel
fun IRequest.isDartVmRequest(): Boolean = this is NetworkRequest

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
            <span ${getColorStyle(color)}>${httpStatusCode}</span> <span ${getColorStyle(secColor)}>${httpMethod}</span> <span ${
        getColorStyle(
            secColor
        )
    }>${durationMs}ms</span>
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
            is String -> if (body.isNotBlank()) body else ""
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
    val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(sizeInBytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}

/**
 * 优化后的 cURL 生成函数
 */
fun IRequest.toCurlStringAsDartDevTools(gsonInstance: Gson? = null): String {
    val isWindows = SystemInfo.isWindows

    // 1. 确定 Shell 环境下的引号和换行符
    // 即使在 Windows 上，我们也倾向于生成 curl.exe 以规避 PowerShell 别名问题
    val cmdBase = if (isWindows) "curl.exe" else "curl"
    val quote = if (isWindows) "\"" else "'"
    val lineContinuation = if (isWindows) " `" else " \\"

    // 2. 增强型转义函数
    fun escapeForShell(text: String): String {
        return if (isWindows) {
            // Windows 双引号内：转义双引号
            // 特殊情况：PowerShell 里的 $ 需要转义，但 curl.exe 本身在 CMD 运行不需要。
            // 综合考虑，转义双引号是最基础的。
            text.replace("\"", "\\\"")
        } else {
            // Unix 单引号内：将 ' 替换为 '\'' (结束当前单引号，转义单引号，开始新单引号)
            text.replace("'", "'\\''")
        }
    }

    val commandParts = mutableListOf<String>()

    // 3. HTTP 方法
    httpMethod?.let {
        commandParts.add("--request ${it.uppercase()}")
    }

    // 4. 处理 URL 和多值查询参数
    val finalUrl = buildString {
        append(requestUrl)
        if (queryParams.isNotEmpty()) {
            val queryString = queryParams.entries.flatMap { (key, value) ->
                val encodedKey = URLEncoder.encode(key, "UTF-8")
                // 处理 List 类型的多值参数，例如 id=1&id=2
                val values = when (value) {
                    is Iterable<*> -> value
                    is Array<*> -> value.toList()
                    else -> listOf(value)
                }
                values.map { v ->
                    val encodedValue = URLEncoder.encode(v?.toString() ?: "", "UTF-8")
                    "$encodedKey=$encodedValue"
                }
            }.joinToString("&")

            if (queryString.isNotEmpty()) {
                append(if (requestUrl.contains("?")) "&" else "?")
                append(queryString)
            }
        }
    }
    commandParts.add("$quote${escapeForShell(finalUrl)}$quote")

    // 5. 添加通用的配置参数
    commandParts.add("--location")
    commandParts.add("--compressed")

    // 6. 添加请求头
    val headersToFilter = setOf("host", "content-length") // 允许 accept-encoding，因为 curl 可以处理
    httpRequestHeaders
        .filterKeys { key -> !headersToFilter.contains(key.lowercase()) }
        .forEach { (key, value) ->
            val escapedValue = escapeForShell(value.toString())
            commandParts.add("--header $quote$key: $escapedValue$quote")
        }

    // 7. 处理请求体
    httpRequestBody?.let { body ->
        val contentType = httpRequestHeaders.entries
            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value?.toString()?.lowercase() ?: ""

        when {
            contentType.contains("application/json") -> {
                val jsonBody = when (body) {
                    is String -> body
                    else -> (gsonInstance ?: GsonBuilder().disableHtmlEscaping().create()).toJson(body)
                }
                commandParts.add("--data-raw ${quote}${escapeForShell(jsonBody)}${quote}")
            }
            contentType.contains("application/x-www-form-urlencoded") -> {
                val formData = if (body is Map<*, *>) {
                    body.map { (k, v) ->
                        "${URLEncoder.encode(k.toString(), "UTF-8")}=${URLEncoder.encode(v?.toString() ?: "", "UTF-8")}"
                    }.joinToString("&")
                } else body.toString()
                commandParts.add("--data ${quote}${escapeForShell(formData)}${quote}")
            }
            else -> {
                commandParts.add("--data-binary ${quote}${escapeForShell(body.toString())}${quote}")
            }
        }
    }

    // 8. 组装命令
    return "$cmdBase " + commandParts.joinToString(separator = "$lineContinuation\n  ")
}



/**
 * 将 IRequest 转换为 PowerShell 的 Invoke-RestMethod 命令字符串
 */
fun IRequest.toPowerShellString(gsonInstance: Gson? = null): String {
    val gson = gsonInstance ?: GsonBuilder().setPrettyPrinting().create()
    val indent = "    " // 用于美化输出的缩进
    val lineContinuation = " `" // PowerShell 的换行符

    return buildString {
        append("Invoke-RestMethod")
        append(lineContinuation)
        append("\n")

        // 1. Method
        val method = (httpMethod ?: "GET").uppercase()
        append("$indent-Method $method")
        append(lineContinuation)
        append("\n")

        // 2. Uri (处理查询参数)
        val finalUrl = buildString {
            append(requestUrl)
            if (queryParams.isNotEmpty()) {
                val queryString = queryParams.map { (key, value) ->
                    val encodedKey = URLEncoder.encode(key, "UTF-8")
                    val encodedValue = URLEncoder.encode(value?.toString() ?: "", "UTF-8")
                    "$encodedKey=$encodedValue"
                }.joinToString("&")

                if (requestUrl.contains("?")) {
                    append("&$queryString")
                } else {
                    append("?$queryString")
                }
            }
        }
        // PowerShell 中 URL 如果包含 $ 符号需要转义，这里简单处理
        val escapedUrl = finalUrl.replace("$", "`$")
        append("$indent-Uri \"$escapedUrl\"")

        // 3. Headers
        if (httpRequestHeaders.isNotEmpty()) {
            append(lineContinuation)
            append("\n")
            append("$indent-Headers @{")
            append("\n")
            httpRequestHeaders.forEach { (key, value) ->
                // 转义头信息中的双引号
                val escapedValue = value.toString().replace("\"", "`\"")
                append("$indent$indent\"$key\" = \"$escapedValue\"\n")
            }
            append("$indent}")
        }

        // 4. Body (如果是 POST/PUT/PATCH 且有请求体)
        val methodsWithBody = listOf("POST", "PUT", "PATCH", "DELETE")
        if (method in methodsWithBody && httpRequestBody != null) {
            append(lineContinuation)
            append("\n")

            val bodyString = when (val body = httpRequestBody) {
                is String -> body
                else -> gson.toJson(body)
            }

            // 在 PowerShell 双引号字符串中，内部双引号建议使用 "" 来表示
            val escapedBody = bodyString.replace("\"", "\"\"")

            append("$indent-ContentType \"application/json; charset=utf-8\"")
            append(lineContinuation)
            append("\n")
            append("$indent-Body \"$escapedBody\"")
        }
    }
}
