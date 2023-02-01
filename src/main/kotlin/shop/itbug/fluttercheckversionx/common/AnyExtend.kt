package shop.itbug.fluttercheckversionx.common

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

fun Any.toJsonFormart() : String {
   return JSONObject.toJSONString(this,JSONWriter.Feature.PrettyFormat)
}

fun String.getVirtualFile() : VirtualFile? {
 return  LocalFileSystem.getInstance().findFileByPath(this)
}