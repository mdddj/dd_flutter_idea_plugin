package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import shop.itbug.fluttercheckversionx.form.socket.SocketRequestForm


/**
 * dio 的请求监听窗口, 在这个窗口中,会将手机里面的一系列请求在这个窗口中显示,并可以查看详细信息
 * 梁典典: 2022年04月29日11:12:51
 */
class SocketWindow : ToolWindowFactory {

    override fun createToolWindowContent(p0: Project, p1: ToolWindow) {
        //dio 监听窗口
        val socketRequestForm = SocketRequestForm(p0,p1)
        val instance = ContentFactory.getInstance()
        val createContent = instance.createContent(socketRequestForm.getContent(), "Dio Request Apis Window", false)
        p1.contentManager.addContent(createContent)

        //在线聊天窗口
        val flutterChatWindow = FlutterChatMessageWindow(p0)
        val flutterChatWindowContent = instance.createContent(flutterChatWindow,"Flutter疑难解答",false)
        p1.contentManager.addContent(flutterChatWindowContent)


    }

}