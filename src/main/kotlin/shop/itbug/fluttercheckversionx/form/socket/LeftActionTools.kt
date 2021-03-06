package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.MyNotifactionUtil
import java.awt.CardLayout
import java.util.function.Supplier
import javax.swing.JPanel

typealias RequestSort = (state: Boolean) -> Unit


//左侧工具栏操作区域
class LeftActionTools(
    project: Project,
    reqList: JBList<Request>,
    rightCardPanel: JPanel,
    private val requestDetailPanel: RequestDetailPanel,
    responseBodyPanel: RightDetailPanel,
    requestSort: RequestSort,
) : DefaultActionGroup() {

    private val deletButton = DeleButton()
    private var sortAction = MySortToggleAction(requestSort)
    private val sortOption = SortAction(action = sortAction)
    private var detailAction = object : ToggleAction("查看请求头", "查看详细信息", AllIcons.Ide.ConfigFile) {
        var selected = false
        override fun isSelected(e: AnActionEvent): Boolean {
            return selected
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            val selectItem = reqList.selectedValue
            selectItem?.let {
                val cardLayout = (rightCardPanel.layout as CardLayout)
                selected = state
                if (state) {
                    changeRequestInDetail(it)
                    cardLayout.show(rightCardPanel, "right_detail_panel")
                } else {
                    responseBodyPanel.changeShowValue(it)
                    cardLayout.show(rightCardPanel, "response_body_panel")
                }
            }
        }
    }

    //复制api url 到剪贴板
    private val copyAction = object : AnAction("复制接口", "将选中的请求的接口链接复制到系统的剪贴板", AllIcons.Actions.Copy) {
        override fun actionPerformed(e: AnActionEvent) {
            val url = reqList.selectedValue?.url
            url?.copyTextToClipboard()
            MyNotifactionUtil.socketNotif(
                if (url == null) "复制失败(未选中接口)" else "复制成功:${url}",
                project = project,
                type = if (url == null) NotificationType.WARNING
                else NotificationType.INFORMATION
            )
        }
    }

    /**
     * 更新详情面板的html数据
     */
    fun changeRequestInDetail(request: Request) {
        requestDetailPanel.changeRequest(request)
    }

    init {
        add(deletButton.action)
        add(sortOption.action)
        addSeparator()
        add(detailAction)
        add(copyAction)
    }


    ///是否在详情页面
    val isInDetailView get() = detailAction.selected

    ///是否勾选了自动滚动到底部的开关
    fun isSelect(): Boolean {
        return sortAction.s
    }

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
    ToggleAction(
        Supplier { "滚动条自动滚动到最底部" },
        Supplier { "当接口监听到数据后,滚动条会一直保持在最底部,关闭后保持不变" },
        AllIcons.ObjectBrowser.SortByType
    ) {
    var s = true
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

