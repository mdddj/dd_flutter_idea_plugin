package shop.itbug.fluttercheckversionx.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import shop.itbug.fluttercheckversionx.services.impl.isObject
import java.math.BigDecimal


fun JsonArray.findPropertiesMaxLenObject(): JsonElement {
    var obj = this.first()
    for (it in this) {
        if (it.isObject) {
            val keys = it.jsonObject.keys
            if (keys.size > obj.jsonObject.keys.size) {
                obj = it
            }
        }
    }
    return obj
}


object DartJavaCovertUtil {
    fun getDartType(obj: Any, key: String): String {
        if (obj is Boolean) {
            return "bool"
        }
        return when (obj::class.java) {
            Integer::class.java -> {
                "int"
            }

            String::class.java -> {
                "String"
            }

            BigDecimal::class.java -> {
                "double"
            }

            Boolean::class.java -> {
                "bool"
            }

            JsonElement::class.java -> {
                key.formatDartName()
            }

            JsonArray::class.java -> {

                val arr = obj as JsonArray

                if (arr.isNotEmpty()) {
                    "List<${key.formatDartName()}>"
                }
                "List<${if (arr.isEmpty()) "dynamic" else getDartType(arr.first(), key)}>"

            }

            else -> {
                "dynamic"
            }
        }
    }
}