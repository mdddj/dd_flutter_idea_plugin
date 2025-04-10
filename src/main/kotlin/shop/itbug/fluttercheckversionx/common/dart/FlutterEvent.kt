package shop.itbug.fluttercheckversionx.common.dart

import com.google.gson.Gson
import com.google.gson.JsonParser
import shop.itbug.fluttercheckversionx.actions.isValidJson


object FlutterEventFactory {

    fun formJsonText(text: String): FlutterEvent? {
        if (!isValidJson(text)) return null
        return try {
            val jsonArray = JsonParser.parseString(text).asJsonArray
            if (jsonArray.size() > 0) {
                val firstObject = jsonArray.get(0).asJsonObject
                return Gson().fromJson(firstObject, FlutterEvent::class.java)
            }
            return null
        } catch (_: Exception) {
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
    val baseUri: String?
)


///获取 ws 地址,eg: ws://127.0.0.1:59388/4u508VtYUpY=/ws
fun FlutterEvent.tryGetWSUrl(): String? {
    if (event == "app.debugPort") {
        return params?.wsUri
    }
    return null
}
