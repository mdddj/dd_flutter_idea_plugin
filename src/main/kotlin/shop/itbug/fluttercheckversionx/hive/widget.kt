package shop.itbug.fluttercheckversionx.hive

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.actions.context.HelpContextAction
import shop.itbug.fluttercheckversionx.actions.context.SiteDocument
import shop.itbug.fluttercheckversionx.hive.component.HiveBoxListComponent
import shop.itbug.fluttercheckversionx.hive.component.HiveValueComponent

///hive工具窗口
class HiveWidget(project: Project, toolWindow: ToolWindow) : BorderLayoutPanel() {


    private val boxAndKeys = HiveBoxListComponent(project)
    private val jsonRender = HiveValueComponent(project).apply {
        border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
    }


    private val mainPanel = OnePixelSplitter().apply {
        firstComponent = boxAndKeys
        secondComponent = jsonRender
        splitterProportionKey = "HiveKey"
    }


    //操作栏
    private val toolbar = ActionManager.getInstance().createActionToolbar(
        "Hive Tool Bar",
        (ActionManager.getInstance()
            .getAction("shop.itbug.fluttercheckversionx.hive.action.HiveDefaultActionGroup") as DefaultActionGroup).apply {
            add(HelpContextAction(SiteDocument.Hive))
        },
        true
    ).apply {
        targetComponent = toolWindow.component
    }

    init {
        addToTop(toolbar.component)
        addToCenter(mainPanel)
    }


}