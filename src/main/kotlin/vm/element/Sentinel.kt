package vm.element

import com.google.gson.JsonObject

/**
 * [Sentinel] 用于指示正常响应不可用。
 */
@Suppress("unused")
class Sentinel(json: JsonObject) : Response(json) {

    /**
     * 这是什么类型的哨兵？
     */
    val kind: SentinelKind
        get() {
            val value = json.get("kind")
            return try {
                if (value == null) SentinelKind.Unknown else SentinelKind.valueOf(value.asString)
            } catch (e: IllegalArgumentException) {
                SentinelKind.Unknown
            }
        }

    /**
     * 此哨兵的合理字符串表示形式。
     */
    val valueAsString: String?
        get() = getAsString("valueAsString")
}
