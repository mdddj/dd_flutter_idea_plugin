package shop.itbug.fluttercheckversionx.actions

import groovy.json.JsonException
import kotlinx.serialization.json.Json

fun isValidJson(jsonString: String): Boolean {
    if (jsonString.isBlank()) return false
    return try {
        Json.parseToJsonElement(jsonString)
        true
    } catch (_: JsonException) {
        false
    } catch (_: Exception) {
        false
    }
}