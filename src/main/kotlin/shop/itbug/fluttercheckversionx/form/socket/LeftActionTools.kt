package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import shop.itbug.fluttercheckversionx.actions.OpenSettingAnAction
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.common.MyToggleAction
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.CardLayout
import javax.swing.JPanel


//左侧工具栏操作区域
class LeftActionTools(
    val project: Project,
    val reqList: JBList<Request>,
    rightCardPanel: JPanel,
    private val requestDetailPanel: RequestDetailPanel,
) : DefaultActionGroup() {

    private val deleteButton = DelButton()


    //查看请求头的工具
    private var detailAction = object : MyToggleAction(
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
                    FlutterApiClickBus.fire(it)
                    cardLayout.show(rightCardPanel, "response_body_panel")
                }
            }
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
        addSeparator()
        add(detailAction)
        add(DioWindowSettingGroup().create("设置").actionGroup)
    }


    ///是否在详情页面
    val isInDetailView get() = detailAction.selected


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



///设置菜单
class DioWindowSettingGroup : DefaultActionGroup({ "设置" }, { "设置相关" }, MyIcons.setting) {

    init {
        isPopup = true
        add(OpenSettingAnAction.getInstance())
    }
}


fun DefaultActionGroup.create(place: String): ActionPopupMenu {
    return ActionManager.getInstance().createActionPopupMenu(place, this,)
}
fun DefaultActionGroup.createWithToolbar(place: String,horizontal: Boolean = true) : ActionToolbar {
   return ActionManager.getInstance().createActionToolbar(place,this,horizontal)
}

