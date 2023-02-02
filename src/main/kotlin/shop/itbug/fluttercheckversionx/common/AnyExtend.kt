package shop.itbug.fluttercheckversionx.common

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.dialog.FreezedClassesGenerateDialog
import shop.itbug.fluttercheckversionx.services.impl.ModelToFreezedModelServiceImpl
import shop.itbug.fluttercheckversionx.util.toastWithError

fun Any.toJsonFormart() : String {
   return JSONObject.toJSONString(this,JSONWriter.Feature.PrettyFormat)
}

fun String.getVirtualFile() : VirtualFile? {
 return  LocalFileSystem.getInstance().findFileByPath(this)
}

fun Project.jsonToFreezedRun(jsonText: String) {
    try {
        val jsonObject = JSONObject.parseObject(jsonText)
        val jsonObjectToFreezedCovertModelList = ModelToFreezedModelServiceImpl().jsonObjectToFreezedCovertModelList(jsonObject)
        FreezedClassesGenerateDialog(this,jsonObjectToFreezedCovertModelList).show()
    }catch (e: Exception) {
        this.toastWithError("转模型失败:$e")
    }
}
