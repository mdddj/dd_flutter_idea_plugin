package shop.itbug.fluttercheckversionx.common

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.dialog.FreezedClassesGenerateDialog
import shop.itbug.fluttercheckversionx.services.impl.ModelToFreezedModelServiceImpl
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.JComponent

fun Any.toJsonFormart(): String {
    return JSONObject.toJSONString(this, JSONWriter.Feature.PrettyFormat)
}

/**
 * 设置为滚动面板
 */
fun JComponent.scroll(): JComponent {
    return JBScrollPane(this)
}

fun String.getVirtualFile(): VirtualFile? {
    return LocalFileSystem.getInstance().findFileByPath(this)
}

fun Project.jsonToFreezedRun(jsonText: String) {
    try {
        val jsonObject = JSON.parseObject(jsonText, JSONReader.Feature.SupportArrayToBean)
        val jsonObjectToFreezedCovertModelList =
            ModelToFreezedModelServiceImpl().jsonObjectToFreezedCovertModelList(jsonObject)
        FreezedClassesGenerateDialog(this, jsonObjectToFreezedCovertModelList).show()
    } catch (e: Exception) {
        println("json to freezed error:$e")
        toastWithError("$e")
        e.printStackTrace()
    }
}
