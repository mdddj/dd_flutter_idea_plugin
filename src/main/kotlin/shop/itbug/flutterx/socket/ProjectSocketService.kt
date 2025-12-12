package shop.itbug.flutterx.socket

import com.google.gson.annotations.SerializedName
import shop.itbug.flutterx.model.IRequest
import shop.itbug.flutterx.model.formatDate
import java.time.LocalDateTime

typealias Request = SocketResponseModel


/**
 * 解析flutter发送过来的模型 (已实现 IRequest 接口)
 */
class SocketResponseModel(
    val data: Any? = null,
    val method: String? = "",
    override val queryParams: Map<String, Any?> = emptyMap(),
    val url: String = "",
    val statusCode: Int = -1,
    val body: Any? = emptyMap<String, Any>(),
    val headers: Map<String, Any> = emptyMap(),
    val responseHeaders: Map<String, Any> = emptyMap(),
    var timestamp: Int = -1,
    var projectName: String = "",
    @SerializedName("createDate") var createDate: String = LocalDateTime.now().formatDate(),
    var extendNotes: List<String> = emptyList()
) : IRequest { // <--- 实现接口

    override val requestUrl: String get() = url
    override val httpMethod: String? get() = method
    override val httpStatusCode: Int get() = statusCode
    override val durationMs: Long get() = timestamp.toLong()
    override val httpRequestHeaders: Map<String, Any> get() = headers
    override val httpResponseHeaders: Map<String, Any> get() = responseHeaders
    override val httpRequestBody: Any? get() = body
    override val httpResponseBody: Any? get() = data
    override val requestStartTime: String get() = createDate

}
