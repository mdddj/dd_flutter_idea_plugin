package window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import form.socket.SocketRequestForm

class SocketWindow : ToolWindowFactory {

    override fun createToolWindowContent(p0: Project, p1: ToolWindow) {
        val socketRequestForm = SocketRequestForm(p1)
        val instance = ContentFactory.SERVICE.getInstance()
        val createContent = instance.createContent(socketRequestForm.content, "", false)
        p1.contentManager.addContent(createContent)

    }

    //    是否显示窗口, 仅在打开项目是判断一次
    override fun isApplicable(project: Project): Boolean {
        return super.isApplicable(project)
    }
}