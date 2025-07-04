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


