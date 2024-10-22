package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import kotlinx.serialization.json.JsonElement
import shop.itbug.fluttercheckversionx.dialog.freezed.StringToFreezedDialog
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.BorderFactory
import javax.swing.JComponent


/**
 * 设置为滚动面板
 */
fun JComponent.scroll(): JBScrollPane {
    return JBScrollPane(this).apply {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
    }
}

///json转 freezed 通用函数
fun Project.jsonToFreezedRun(jsonText: String) {
    StringToFreezedDialog(this, jsonText).show()
//    val jsonObject = jsonText.getParseJsonObject()
//    jsonObject?.let {
//        jsonToFreezedRun2(it)
//    }
}


private fun Project.jsonToFreezedRun2(jsonObject: JsonElement) {
    try {

//        val jsonObjectToFreezedCovertModelList =
//            ModelToFreezedModelServiceImpl().jsonObjectToFreezedCovertModelList(jsonObject)
//        FreezedClassesGenerateDialog(this, jsonObjectToFreezedCovertModelList).show()
    } catch (e: Exception) {
        println("json to freezed error:$e")
        toastWithError("$e")
        e.printStackTrace()
    }
}