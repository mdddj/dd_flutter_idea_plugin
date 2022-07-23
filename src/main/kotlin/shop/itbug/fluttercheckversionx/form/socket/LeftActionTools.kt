package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import shop.itbug.fluttercheckversionx.dialog.RequestDetailDialog
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.util.function.Supplier

typealias RequestSort = (state: Boolean) -> Unit


//左侧工具栏操作区域
class LeftActionTools(
    project: Project,
    reqList: JBList<Request>,
    requestSort: RequestSort,
) : DefaultActionGroup() {

    private val deletButton = DeleButton()
    private var sortAction = MySortToggleAction(requestSort)
    private val sortOption = SortAction(action = sortAction)

    init {
        add(deletButton.action)
        add(sortOption.action)
        addSeparator()
        add(object : AnAction("查看请求头","查看详细信息",AllIcons.Ide.ConfigFile){
            override fun actionPerformed(e: AnActionEvent) {
                val selectItem = reqList.selectedValue
                selectItem?.let {
                    RequestDetailDialog.show(project,selectItem)
                }
            }
        })
    }

    fun isSelect(): Boolean{ return sortAction.s}

    companion object {
        fun create(modl: LeftActionTools): ActionToolbar {
            return ActionManager.getInstance().createActionToolbar(
                "Dio Tool Left Action",
                modl,
                false
            )
        }
    }
}



class DeleButton : ActionButton(
    object : AnAction("清除记录", "清除列表中的全部历史记录", AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) {
            service<AppService>().cleanAllRequest()
        }
    },
    Presentation("清除全部记录"),
    "Dio Tool Left Action Sort",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
)

class MySortToggleAction(private val handle: RequestSort) :
    ToggleAction(Supplier { "11" }, Supplier { "22" }, AllIcons.ObjectBrowser.SortByType) {
    var s = false
    override fun isSelected(e: AnActionEvent): Boolean {
        return s
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        handle.invoke(state)
        s = state
    }

}

class SortAction(action: MySortToggleAction) : ActionButton(
    action,
    Presentation("使用倒序的方式渲染列表"),
    "Dio Tool Left Action Sort",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
)

