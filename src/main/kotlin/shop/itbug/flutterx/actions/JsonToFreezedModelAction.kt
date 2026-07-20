package shop.itbug.flutterx.actions

import kotlinx.serialization.json.Json

fun isValidJson(jsonString: String): Boolean {
    return jsonString.isNotBlank() && try {
        Json.parseToJsonElement(jsonString)
        true
    } catch (_: Exception) {
        false
    }
}