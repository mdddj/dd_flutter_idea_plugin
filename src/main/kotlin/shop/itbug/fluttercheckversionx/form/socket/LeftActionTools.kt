package shop.itbug.fluttercheckversionx.form.socket

import cn.hutool.core.net.url.UrlBuilder
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.dialog.SimpleJsonViewDialog
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
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

    private val deleteButton = DelButton()
    private var sortAction = MySortToggleAction(requestSort)
    private val sortOption = SortAction(action = sortAction)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    //查看请求头的工具
    private var detailAction = object : ToggleAction(
        PluginBundle.get("window.idea.dio.view.detail"),
        PluginBundle.get("window.idea.dio.view.detail.desc"),
        AllIcons.General.ShowInfos
    ) {
        var selected = false
        override fun isSelected(e: AnActionEvent): Boolean {
            return selected
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
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
                MyNotificationUtil.socketNotif(
                    if (url == null) "复制失败(未选中接口)" else "复制成功:${url}",
                    project = project,
                    type = if (url == null) NotificationType.WARNING
                    else NotificationType.INFORMATION
                )
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.EDT
            }
        }

    private val viewQueryParamsAction = ViewGetQueryParamsAction(reqList, project = project)

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
        add(viewQueryParamsAction)
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


class DelButton : ActionButton(
    object :
        AnAction(PluginBundle.get("clean"), PluginBundle.get("window.idea.dio.view.clean.desc"), AllIcons.Actions.GC) {
        override fun actionPerformed(e: AnActionEvent) {
            service<AppService>().cleanAllRequest()
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
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

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}

class SortAction(action: MySortToggleAction) : ActionButton(
    action,
    Presentation("使用倒序的方式渲染列表"),
    "Dio Tool Left Action Sort",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
)


///查看get方法下,queryparams参数的功能
class ViewGetQueryParamsAction(private val reqList: JBList<Request>, private val project: Project) :
    AnAction(
        PluginBundle.get("window.idea.dio.view.query.params"),
        PluginBundle.get("window.idea.dio.view.query.params.desc"),
        AllIcons.Ide.ConfigFile
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        if (reqList.selectedValue != null) {
            val url = UrlBuilder.ofHttp(reqList.selectedValue.url)
            val queryMap = url.query.queryMap
            SimpleJsonViewDialog.show(queryMap, project)
        } else {
            MyNotificationUtil.socketNotif(
                PluginBundle.get("window.idea.dio.view.query.params.tip"),
                project,
                NotificationType.WARNING
            )
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}


