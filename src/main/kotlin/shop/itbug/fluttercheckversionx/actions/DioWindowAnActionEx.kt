package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import shop.itbug.fluttercheckversionx.form.components.ApiListPanel
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.socket.service.AppService

fun AnActionEvent.api(): Request? {
//    return getData(DataKey.create(ApiListPanel.SELECT_KEY))
    return service<AppService>().currentSelectApi
}

 fun AnActionEvent.apiListProject() : Project? {
     println(">>222>${presentation.getClientProperty(Key<Request?>(ApiListPanel.SELECT_KEY))}")
    return getData(CommonDataKeys.PROJECT)
}