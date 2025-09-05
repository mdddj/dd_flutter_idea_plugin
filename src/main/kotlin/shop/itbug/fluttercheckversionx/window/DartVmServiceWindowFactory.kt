package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import shop.itbug.fluttercheckversionx.common.dart.isSupportDartVm
import shop.itbug.fluttercheckversionx.window.DartVmServiceWindowFactory.Companion.ID
import shop.itbug.fluttercheckversionx.window.flutter.WidgetTreeWindow

class DartVmServiceWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project, toolWindow: ToolWindow
    ) {
        // widget tree
        val flutterWidgetTree = WidgetTreeWindow(project)
        val flutterWidgetTreeContent =
            ContentFactory.getInstance().createContent(flutterWidgetTree, "Widget Tree", false)
        flutterWidgetTreeContent.setDisposer(flutterWidgetTree)
        toolWindow.contentManager.addContent(flutterWidgetTreeContent)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return isSupportDartVm
    }
    companion object {
        const val ID = "FlutterX VM Service"
    }
}

// 获取 dart vm service 窗口
fun getFlutterXVmServiceToolWindow(project: Project): ToolWindow {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ID)
    return toolWindow!!
}

fun showDartVmServiceToolWindow(project: Project) {
    val toolWindow = getFlutterXVmServiceToolWindow(project)
    if (!toolWindow.isVisible) {
        toolWindow.show()
    }
}

fun dartVmServiceWindowIsShow(project: Project): Boolean {
    return getFlutterXVmServiceToolWindow(project).isVisible
}