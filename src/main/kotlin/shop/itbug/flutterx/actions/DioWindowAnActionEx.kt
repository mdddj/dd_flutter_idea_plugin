package shop.itbug.flutterx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.form.components.ApiListPanel
import shop.itbug.flutterx.model.IRequest
import shop.itbug.flutterx.util.ComposeHelper


///获取当前选中的项目
fun AnActionEvent.api(): IRequest? {
    val data = getData(ApiListPanel.SELECT_ITEM)
    val dioApi = data as? IRequest?
    if (dioApi != null) return dioApi
    val dart = getData(ComposeHelper.networkRequestDataKey)
    
    return dart as? IRequest?
}
