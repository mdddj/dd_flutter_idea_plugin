package shop.itbug.fluttercheckversionx.actions

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.socket.Request


fun Request.getDataJson(): Any? {
    if (data == null) {
        return null
    }
    if (data is Map<*, *>) {
        return data
    }
    if (data is String && data.isNotBlank()) {
        return try {
            Gson().fromJson(data, Map::class.java)
        } catch (_: Exception) {
            data
        }
    }
    return data
}

///获取当前选中的项目
fun AnActionEvent.api(): Request? {
    return getData(ApiListPanel.SELECT_ITEM)
}
