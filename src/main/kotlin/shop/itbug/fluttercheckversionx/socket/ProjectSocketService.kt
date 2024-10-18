package shop.itbug.fluttercheckversionx.socket

import com.google.gson.annotations.SerializedName
import com.intellij.openapi.ui.MessageType
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.actions.getDataJson
import shop.itbug.fluttercheckversionx.config.DioCopyAllKey
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.util.toHexString
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class TestRequestBody(
    var hello: String = "test",
    var test: Int = 1,
    var floatValue: Float = 1.2f,
    var longValue: Long = 12000L,
    var boolValue: Boolean = true,
)


class ProjectSocketService {
    companion object {
        fun getTestApi(): Request {
            return Request(
                data = mapOf("testInt" to 1, "testDouble" to 100.2, "testBool" to false, "object" to TestRequestBody()),
                method = "GET",
                queryParams = emptyMap(),
                url = "https://itbug.shop:6666/api/test?hello=1&test=true",
                statusCode = 200,
                body = null,
                headers = emptyMap(),
                responseHeaders = emptyMap(),
                timestamp = 300,
                projectName = "Test Request",
                createDate = LocalDateTime.now().toString(),
            )
        }
    }

    /**
     * 解析flutter发送过来的模型
     */
    class SocketResponseModel(
        ///服务器返回谁
        val data: Any? = null,
        ///请求类型
        val method: String? = "",
        /// get 查询参数
        val queryParams: Map<String, Any> = emptyMap(),
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
    }

}


fun LocalDateTime.formatDate(): String {
    val f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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