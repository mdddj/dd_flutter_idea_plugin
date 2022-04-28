package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import shop.itbug.fluttercheckversionx.form.socket.SocketRequestForm

class SocketWindow : ToolWindowFactory {

    override fun createToolWindowContent(p0: Project, p1: ToolWindow) {
        val socketRequestForm = SocketRequestForm(p0)
        val instance = ContentFactory.SERVICE.getInstance()
        val createContent = instance.createContent(socketRequestForm.getContent(), "", false)
        p1.contentManager.addContent(createContent)

    }

}