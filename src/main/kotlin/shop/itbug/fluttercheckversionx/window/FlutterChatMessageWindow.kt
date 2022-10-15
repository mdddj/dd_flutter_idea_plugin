package shop.itbug.fluttercheckversionx.window

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.fields.ExtendableTextField
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToolBar

/**
 * Flutter聊天窗口
 */
class FlutterChatMessageWindow(val project: Project,val toolWindow: ToolWindow) : JPanel(BorderLayout()) {


    private val topMessageLabel = JLabel("开发中,敬请期待,意见加QQ群:706438100")


    private val topToolBar = JToolBar()//顶部操作区域的工具栏
    private val chatList = JBList<Any>()//聊天显示区域
    private val userAvatar = JButton("登录/注册",AllIcons.General.User)//头像
    private val bottomToolBar = JToolBar()//底部工具栏
    private val chatTextField = ExtendableTextField()//聊天内容编辑框
    private val sendButton = JButton("发送") //发送按钮

    init {
        uiInit()
    }


    private fun uiInit() {
        topToolBar.apply {
            isFloatable = false
            add(userAvatar)
        }
        bottomToolBar.apply {
            isFloatable = false
            add(chatTextField)
            add(sendButton)
        }
        chatTextField.apply {
            emptyText.text = "说点什么吧"
        }
        chatList.apply {
            emptyText.appendLine("暂时没有人聊天")
        }
        add(topToolBar,BorderLayout.NORTH)
        add(chatList,BorderLayout.CENTER)
        add(bottomToolBar,BorderLayout.SOUTH)
    }
}