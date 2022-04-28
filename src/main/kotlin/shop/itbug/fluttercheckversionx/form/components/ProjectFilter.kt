package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import shop.itbug.fluttercheckversionx.services.SokcetMessageBus
import shop.itbug.fluttercheckversionx.socket.service.AppService
import javax.swing.JComponent

typealias SelectProject = (projectName: String) -> Unit


/// 项目过滤
class ProjectFilter(val selectItem: SelectProject): ComboBoxAction() {


    init {

        /// 监听新的请求到来,刷洗一下UI
        ApplicationManager.getApplication().messageBus.connect().subscribe(SokcetMessageBus.CHANGE_ACTION_TOPIC,SokcetMessageBus{
            this.createPopupActionGroup(null)
        })

    }



    override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup {
        val actions = DefaultActionGroup()
        val service = service<AppService>()
        val allProjectNames = service.getAllProjectNames()

        for (item in allProjectNames) {
            actions.add(object : AnAction(item){
                override fun actionPerformed(e: AnActionEvent) {
                    selectItem(item)
                }
            })
        }

        return actions
    }



}