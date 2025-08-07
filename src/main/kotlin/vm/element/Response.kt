package vm.element

import com.google.gson.JsonObject

/**
 * Service Protocol 返回的每个非错误响应都扩展 [Response]。通过使用
 * [type] 属性，客户端可以确定提供了哪种类型的响应。
 */
@Suppress("unused")
open class Response(json: JsonObject) : Element(json) {

    /**
     * VM Service 返回的每个响应都有 type 属性。这允许客户端
     * 区分不同类型的响应。
     */
    val type: String
        get() = getAsString("type") ?: ""


    override fun toString(): String {
        return json.toString()
    }
}
