package shop.itbug.fluttercheckversionx.form.sub

import com.google.gson.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import java.lang.reflect.Type


internal class GsonIntAdapter : JsonSerializer<Int?>, JsonDeserializer<Int?> {
    override fun serialize(p0: Int?, p1: Type?, p2: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(p0)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Int {
        try {
            println("类型:$typeOfT")
            return json.asInt
        } catch (e: NumberFormatException) {
            throw JsonParseException("Failed to parse Integer value: " + json.asString, e)
        }
    }
}

/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */

open class JsonValueRender(p: Project) : JsonEditorTextPanel(p) {


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?) {
        if (json != null && !project.isDisposed) {
            val changeJson = changeJson(json)
            WriteCommandAction.runWriteCommandAction(project) {
                text = changeJson
            }
            super.scrollToTop()
        }
    }

    private fun isValidJson(jsonString: String?): Boolean {
        try {
            JsonParser.parseString(jsonString)
            return true
        } catch (e: JsonSyntaxException) {
            return false
        }
    }

    /**
     * 改变显示内容
     *
     * 返回要显示的json string
     */
    private fun changeJson(json: Any): String {
        val builder = DioApiService.getInstance().gson

        if (json is String) {
            try {
                if (isValidJson(json)) {
                    val map = builder.fromJson(json, Map::class.java)
                    return builder.toJson(map)
                }
            } catch (e: Exception) {
                println("尝试String转json失败:${e.localizedMessage} \n String is $json")
            }
        }
        if (json is Map<*, *>) {
            try {
                return builder.toJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            return builder.toJson(json)
        } catch (_: Exception) {
        }
        return json.toString()
    }

}


