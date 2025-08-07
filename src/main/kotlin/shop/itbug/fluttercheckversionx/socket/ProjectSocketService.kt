package shop.itbug.fluttercheckversionx.socket

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.actions.getDataJson
import shop.itbug.fluttercheckversionx.config.DioCopyAllKey
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.dsl.formatUrl
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.util.toHexString
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.SwingUtilities

typealias Request = SocketResponseModel

/**
 * 解析flutter发送过来的模型
 */
class SocketResponseModel(
    ///服务器返回谁
    val data: Any? = null,
    ///请求类型
    val method: String? = "",
    /// get 查询参数
    val queryParams: Map<String, Any?> = emptyMap(),
    ///请求URL
    val url: String = "",

    ///状态码
    val statusCode: Int = -1,

    ///post参数
    val body: Any? = emptyMap<String, Any>(),

    ///请求头
    val headers: Map<String, Any> = emptyMap(),

    ///response 请求头
    val responseHeaders: Map<String, Any> = emptyMap(),

    ///请求耗时
    var timestamp: Int = -1,

    ///项目名称
    var projectName: String = "",

    ///时间
    @SerializedName("createDate") var createDate: String = LocalDateTime.now().formatDate(),

    ///扩展label列表
    var extendNotes: List<String> = emptyList()
) {
    val hurlGenerate = HurlGenerate(this)
    override fun toString(): String {
        return "${url}:${statusCode}:${body}:${extendNotes}:${createDate}"
    }

    ///计算大小
    fun calculateSize(): String {
        try {
            if (this.data == null) {
                return "0"
            }
            return formatSize(data.toString().toByteArray().size.toLong())
        } catch (_: Exception) {
            return "0"
        }
    }

    private fun isSuccessful(): Boolean {
        return statusCode == 200
    }

    private fun getInfoColor(): String {
        return UIUtil.getLabelSuccessForeground().toHexString()
    }

    private fun getWarningColor(): String {
        return UIUtil.getErrorForeground().toHexString()
    }

    fun getHtmlPrefix(): String {
        val color = if (isSuccessful()) getInfoColor() else getWarningColor()
        val secColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND.toHexString()
        fun getColorStyle(color: String): String {
            return "style='color:${color}'"
        }
        return """
                <span ${getColorStyle(color)}>${statusCode}</span> <span ${getColorStyle(secColor)}>${method}</span> <span ${
            getColorStyle(
                secColor
            )
        }>${timestamp}ms</span>
            """.trimIndent()
    }

    fun getMessageType(): MessageType {
        return if (isSuccessful()) MessageType.INFO else MessageType.ERROR
    }

    fun getMap(config: DioCopyAllKey): Map<String, Any?> {
        val dataMap = mutableMapOf(
            config.url to url,
            config.method to method,
            config.headers to headers,
        )
        queryParams.apply {
            if (this.isNotEmpty()) {
                dataMap[config.queryParams] = this
            }
        }
        body?.apply {
            dataMap[config.body] = this
        }
        dataMap[config.responseStatusCode] = statusCode
        dataMap[config.response] = getDataJson()
        dataMap[config.requestTime] = createDate
        dataMap[config.timestamp] = timestamp
        return dataMap
    }


    //在编辑器中打开请求数据
    fun openJsonDataInEditor(project: Project) {
        val config = DioListingUiConfig.setting.copyKeys
        val map = getMap(config)
        val toJson = DioApiService.getInstance().gson.toJson(map)
        val file = LightVirtualFile("request.json", toJson)
        SwingUtilities.invokeLater {
            FileEditorManager.getInstance(project).openFile(file)
        }

    }

}


// hurl生成器
class HurlGenerate(val request: Request) {

    //生成基本的
    fun base(): String {
        return """
        |${request.method?.uppercase(Locale.getDefault())} ${request.formatUrl(DoxListeningSetting(showQueryParams = false))}    
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
        val headers = request.headers

        val str = if (headers.isNotEmpty()) headers
            .filter {
                it.key != "content-length"
            }
            .map {
                println("${it.key} : ${it.value}")
                return@map "${it.key} : ${it.value}"
            }.joinToString("\n") else ""
        return str
    }

    private fun httpSuccess(): String {
        return "HTTP 200"
    }

    private fun getQueryParams(): String {
        val queryParams = request.queryParams
        if (queryParams.isNotEmpty()) {
            return """
            |[Query]
            |${
                queryParams.filter { it.value != null && it.value.toString().isBlank().not() }
                    .map { it.key + " : " + "${it.value}" }.joinToString("\n")
            }
            """.trimMargin().trimIndent()
        }
        return ""
    }

    //post body
    private fun getPostBody(): String {
        return when (request.body) {
            is Map<*, *> if request.body.isNotEmpty() -> {
                GsonBuilder().setPrettyPrinting().create().toJson(request.body)
            }

            else -> ""
        }
    }
}


fun LocalDateTime.formatDate(): String {
    val f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    return this.format(f)
}

fun formatSize(sizeInBytes: Long): String {
    val units = arrayOf("Bytes", "KB", "MB", "GB", "TB")
    var size = sizeInBytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    val df = DecimalFormat("#.##")
    return "${df.format(size)} ${units[unitIndex]}"
}