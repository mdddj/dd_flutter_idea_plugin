package shop.itbug.fluttercheckversionx.common

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter

fun Any.toJsonFormart() : String {
   return JSONObject.toJSONString(this,JSONWriter.Feature.PrettyFormat)
}