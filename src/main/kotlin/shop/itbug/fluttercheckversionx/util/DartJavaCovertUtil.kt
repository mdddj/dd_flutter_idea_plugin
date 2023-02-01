package shop.itbug.fluttercheckversionx.util

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import java.math.BigDecimal

object DartJavaCovertUtil {
    fun getDartType(clazz: Class<*>,key: String) : String {
        println(clazz)
        return when(clazz){
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
                key
            }
            JSONArray::class.java -> {
                "List<$key>"
            }
            else -> {
                "dynamic"
            }
        }
    }
}