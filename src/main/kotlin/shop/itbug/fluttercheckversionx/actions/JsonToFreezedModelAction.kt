package shop.itbug.fluttercheckversionx.actions

import groovy.json.JsonException
import kotlinx.serialization.json.Json

fun isValidJson(jsonString: String): Boolean {
    return try {
        Json.parseToJsonElement(jsonString)
        true
    } catch (_: JsonException) {
        false
    }
}