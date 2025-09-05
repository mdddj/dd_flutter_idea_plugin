package shop.itbug.fluttercheckversionx.common.dart

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import shop.itbug.fluttercheckversionx.actions.isValidJson


object FlutterEventFactory {

    val gson = GsonBuilder().setPrettyPrinting().create()
    fun formJsonText(text: String): FlutterEvent? {
        if(!text.contains("\"event\"")) return null
        if (!isValidJson(text)) return null
        return try {
            val jsonElement = JsonParser.parseString(text)
            
            // 检查是否是数组格式
            if (jsonElement.isJsonArray) {
                val jsonArray = jsonElement.asJsonArray
                if (jsonArray.size() > 0) {
                    val firstObject = jsonArray.get(0).asJsonObject
                    val result = gson.fromJson(firstObject, FlutterEvent::class.java)
                    // 只记录重要事件
                    if (result.event in listOf("app.start", "app.debugPort", "app.started", "app.stop")) {
                        println("FlutterEventFactory: 解析重要事件 ${result.event} - appId: ${result.params?.appId}")
                    }
                    return result
                }
            } else if (jsonElement.isJsonObject) {
                // 处理单个JSON对象
                val jsonObject = jsonElement.asJsonObject
                val result = gson.fromJson(jsonObject, FlutterEvent::class.java)
                // 只记录重要事件
                if (result.event in listOf("app.start", "app.debugPort", "app.started", "app.stop")) {
                    println("FlutterEventFactory: 解析重要对象事件 ${result.event} - appId: ${result.params?.appId}")
                }
                return result
            }
            
            return null
        } catch (e: Exception) {
            // 只记录包含重要事件的解析失败
            if (text.contains("app.debugPort") || text.contains("app.start")) {
                println("FlutterEventFactory: 重要事件解析失败 - ${text.take(100)}..., 错误: ${e.message}")
            }
            null
        }
    }
}

data class FlutterEvent(
    val event: String,
    val params: FlutterEventParams?
)

data class FlutterEventParams(
    val appId: String?,
    val port: Long?,
    val wsUri: String?,
    val baseUri: String?,
    val mode: String?,
    val deviceId: String?,
)


