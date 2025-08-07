package vm.element

import com.google.gson.JsonObject

/**
 * [Success] 类型用于指示操作已成功完成。
 */
@Suppress("unused")
class Success(json: JsonObject) : Response(json)
