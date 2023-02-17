package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import java.awt.CardLayout
import java.util.function.Supplier
import javax.swing.JPanel

typealias RequestSort = (state: Boolean) -> Unit


//左侧工具栏操作区域
class LeftActionTools(
    val project: Project,
    val reqList: JBList<Request>,
    rightCardPanel: JPanel,
    private val requestDetailPanel: RequestDetailPanel,
    responseBodyPanel: RightDetailPanel,
    requestSort: RequestSort,
) : DefaultActionGroup() {

    private val deleteButton = DelButton()
    private var sortAction = MySortToggleAction(requestSort)
    private val sortOption = SortAction(action = sortAction)


    //查看请求头的工具
    private var detailAction = object : ToggleAction(
        PluginBundle.get("window.idea.dio.view.detail"),
        PluginBundle.get("window.idea.dio.view.detail.desc"),
        MyIcons.infos
    ) {
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
    private val copyAction =
        object : AnAction(
            PluginBundle.get("window.idea.dio.view.copy"),
            PluginBundle.get("window.idea.dio.view.copy.desc"),
            AllIcons.Actions.Copy
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                val url = reqList.selectedValue?.url
                url?.copyTextToClipboard()
                MyNotificationUtil.socketNotify(
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
        add(deleteButton.action)
        add(sortOption.action)
        addSeparator()
        add(detailAction)
        add(copyAction)
        add(showParamsActionGroup.actionGroup)
        add(DioWindowSettingGroup().create("设置").actionGroup)
    }


    ///是否在详情页面
    val isInDetailView get() = detailAction.selected

    ///是否勾选了自动滚动到底部的开关
    fun isSelect(): Boolean {
        return sortAction.s
    }

    companion object {
        fun create(model: LeftActionTools): ActionToolbar {
            return ActionManager.getInstance().createActionToolbar(
                "Dio Tool Left Action",
                model,
                false
            )
        }
    }

    private val showParamsActionGroup: ActionPopupMenu
        get() = ActionManager.getInstance()
            .createActionPopupMenu("show-params", ShowParamsActionGroup(reqList = reqList, project = project))
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

//自动滚动到底部
class MySortToggleAction(private val handle: RequestSort) :
    ToggleAction(
        Supplier { PluginBundle.get("dio.toolbar.2.1") },
        Supplier { PluginBundle.get("dio.toolbar.2.2") },
        AllIcons.RunConfigurations.Scroll_down
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

//
class SortAction(action: MySortToggleAction) : ActionButton(
    action,
    Presentation(PluginBundle.get("dio.toolbar.3.1")),
    "Dio Tool Left Action Sort",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
)

///查看get方法下,query params参数的功能
class ViewGetQueryParamsAction(private val reqList: JBList<Request>, private val project: Project) :
    AnAction(
        PluginBundle.get("dio.toolbar.get.params"),
        PluginBundle.get("window.idea.dio.view.query.params.desc"),
        AllIcons.Ide.ConfigFile
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        reqList.selectedValue?.apply {
            this.queryParams?.let { SimpleJsonViewDialog.show(it, project) }
                .takeIf { this.queryParams?.isEmpty() != true }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled =
            reqList.selectedValue != null && reqList.selectedValue?.queryParams?.isNotEmpty() == true
        super.update(e)
    }


}

///查看post方法下,query params参数的功能
class ViewPostQueryParamsAction(reqList: JBList<Request>, private val project: Project) :
    AnAction(
        PluginBundle.get("dio.toolbar.post.params"),
        PluginBundle.get("window.idea.dio.view.query.params.desc"),
        AllIcons.Ide.ConfigFile
    ) {

    private val selectValue = reqList.selectedValue
    override fun actionPerformed(e: AnActionEvent) {
        selectValue?.apply {
            this.body?.let { SimpleJsonViewDialog.show(it, project) }
                .takeIf { this.body is Map<*, *> && this.body.isNotEmpty() }
        }
    }

    override fun update(e: AnActionEvent) {
        val body = selectValue?.body
        e.presentation.isEnabled = selectValue != null && body != null && body is Map<*, *> && body.isNotEmpty()
        super.update(e)
    }

}

///查看参数的选项
class ShowParamsActionGroup(val reqList: JBList<Request>, val project: Project) :
    DefaultActionGroup("请求参数查询", true) {

    private val viewQueryParamsAction = ViewGetQueryParamsAction(reqList, project = project)
    private val viewPostParamsAction = ViewPostQueryParamsAction(reqList, project = project)

    init {

        add(viewQueryParamsAction)
        add(viewPostParamsAction)
        super.getTemplatePresentation().icon = MyIcons.params

        reqList.addListSelectionListener {
        }

    }
}

///设置菜单
class DioWindowSettingGroup() : DefaultActionGroup({ "设置" }, { "设置相关" }, MyIcons.setting) {


    init {
//        add(object : AnAction("服务状态", "查看dio服务状态", AllIcons.Ide.Link) {
//            override fun actionPerformed(e: AnActionEvent) {
//                e.project?.openSocketStatusDialog()
//            }
//        })

        isPopup = true
        add(OpenSettingAnAction.getInstance())

    }
}


fun DefaultActionGroup.create(place: String): ActionPopupMenu {
    return ActionManager.getInstance().createActionPopupMenu(place, this)
}

