package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService


//左侧工具栏操作区域
class LeftActionTools(
    val project: Project,
) : DefaultActionGroup() {

    private val deleteButton = DelButton()



    init {
        add(deleteButton.action)
        add(OpenSettingAnAction.getInstance())
    }



}

//清理
class DelButton : ActionButton(
    object :
        AnAction(PluginBundle.get("clean"), PluginBundle.get("window.idea.dio.view.clean.desc"), AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) {
            service<AppService>().cleanAllRequest()
        }

    },
    Presentation("清除全部记录"),
    "Dio Tool Left Action Sort",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
)




fun DefaultActionGroup.create(place: String): ActionPopupMenu {
    return ActionManager.getInstance().createActionPopupMenu(place, this)
}

fun DefaultActionGroup.createWithToolbar(place: String, horizontal: Boolean = true): ActionToolbar {
    return ActionManager.getInstance().createActionToolbar(place, this, horizontal)
}

