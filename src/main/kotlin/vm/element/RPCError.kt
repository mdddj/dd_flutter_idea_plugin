package vm.element

import com.google.gson.JsonObject
import vm.internal.VmServiceConst

/**
 * 当 RPC 遇到错误时，它会在响应对象的 _error_ 属性中提供。
 * JSON-RPC 错误总是提供 _code_、_message_ 和 _data_ 属性。<br/>
 * 下面是我们上面的 [streamListen](#streamlisten) 请求的示例错误响应。如果我们尝试从
 * 同一个客户端多次订阅 _GC_ 流，则会生成此错误。
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "error": {
 *     "code": 103,
 *     "message": "Stream already subscribed",
 *     "data": {
 *       "details": "The stream 'GC' is already subscribed"
 *     }
 *   }
 *   "id": "2"
 * }
 * </pre>
 * <p>
 * 除了 JSON-RPC 规范中指定的 [错误代码](http://www.jsonrpc.org/specification#error_object) 外，
 * 我们还使用以下特定于应用程序的错误代码：
 *
 * <pre>
 * code | message | meaning
 * ---- | ------- | -------
 * 100 | Feature is disabled | 该功能已禁用，因此无法完成操作
 * 101 | VM must be paused | 此操作仅在 VM 暂停时有效
 * 102 | Cannot add breakpoint | VM 无法在指定的行或函数处添加断点
 * 103 | Stream already subscribed | 客户端已经订阅了指定的 _streamId_
 * 104 | Stream not subscribed | 客户端未订阅指定的 _streamId_
 * </pre>
 */
class RPCError(json: JsonObject) : Element(json), VmServiceConst {
    companion object {
        /**
         * 当客户端收到服务器的响应但未预期到时使用的响应代码。例如，它请求了一个库元素但收到了一个列表。
         */
        const val UNEXPECTED_RESPONSE = 5

        fun unexpected(expectedType: String, response: Response): RPCError {
            var errMsg = "Expected type $expectedType but received ${response.type}"
            if (response is Sentinel) {
                errMsg += ": ${response.kind}"
            }
            val json = JsonObject()
            json.addProperty("code", UNEXPECTED_RESPONSE)
            json.addProperty("message", errMsg)
            val data = JsonObject()
            data.addProperty("details", errMsg)
            data.add("response", response.json)
            json.add("data", data)
            return RPCError(json)
        }
    }

    val code: Int
        get() = json.get("code").asInt

    val details: String?
        get() {
            val data = json.get("data")
            if (data is JsonObject) {
                val details = data.get("details")
                if (details != null) {
                    return details.asString
                }
            }
            return null
        }

    val message: String
        get() = json.get("message").asString

    val request: JsonObject?
        get() {
            val data = json.get("data")
            if (data is JsonObject) {
                val request = data.get("request")
                if (request is JsonObject) {
                    return request
                }
            }
            return null
        }
    val exception get() = DartVMRPCException(this)
}

class DartVMRPCException(val err: RPCError) : Error() {
    override fun getLocalizedMessage(): String {
        return err.message + "\n" + err.details + "\n" + err.json.toString()
    }
}