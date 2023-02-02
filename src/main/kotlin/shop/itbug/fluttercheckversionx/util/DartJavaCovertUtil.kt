package shop.itbug.fluttercheckversionx.util

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import java.math.BigDecimal


fun JSONArray.findPropertiesMaxLenObject() : JSONObject {
    var obj = first() as JSONObject
    forEach {
        if(it is JSONObject) {
            if(it.keys.size > obj.keys.size) {
                obj = it
            }
        }
    }
    return obj
}

object DartJavaCovertUtil {
    fun getDartType(obj: Any,key: String) : String {
        if(obj is Boolean) {
            return "bool"
        }
        return when(obj::class.java){
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
            JSONObject::class.java -> {
                key.formatDartName()
            }
            JSONArray::class.java -> {

                val arr = obj as JSONArray

                if(arr.isNotEmpty()) {
                    if(arr.first() is JSONObject){
                        "List<${key.formatDartName()}>"
                    }

                }
                "List<${getDartType(arr.first(),key)}>"

            }
            else -> {
                "dynamic"
            }
        }
    }
}