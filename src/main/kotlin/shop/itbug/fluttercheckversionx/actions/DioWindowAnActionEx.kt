package shop.itbug.fluttercheckversionx.actions

import com.alibaba.fastjson2.JSON
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.socket.service.AppService


fun Request.getDataJson() : Any {
    if(data is Map<*, *>) {
        return data
    }
    if(data is String && JSON.isValid(data)) {
        return  JSON.parse(data)
    }
    return data ?: ""
}

///获取当前选中的项目
fun AnActionEvent.api(): Request? {
    return service<AppService>().currentSelectRequest
}
