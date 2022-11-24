package shop.itbug.fluttercheckversionx.window

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBList
import com.intellij.ui.components.fields.ExtendableTextField
import shop.itbug.fluttercheckversionx.dsl.loginPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToolBar

/**
 * Flutter聊天窗口
 */
class FlutterChatMessageWindow(val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()) {

    private val topToolBar = JToolBar()//顶部操作区域的工具栏
    private val chatList = JBList<Any>()//聊天显示区域
    private val userAvatarButton = JButton(PluginBundle.get("window.chat.loginAndRegister"),AllIcons.General.User)//头像
    private val bottomToolBar = JToolBar()//底部工具栏
    private val chatTextField = ExtendableTextField()//聊天内容编辑框
    private val sendButton = JButton("发送") //发送按钮

    init {
        uiInit()
    }


    private fun uiInit() {
        topToolBar.apply {
            isFloatable = false
            add(userAvatarButton)
        }
        bottomToolBar.apply {
            isFloatable = false
            add(chatTextField)
            add(sendButton)
        }
        chatTextField.apply {
            emptyText.text = PluginBundle.get("window.chat.sendInput.desc")
        }
        chatList.apply {
            emptyText.appendLine(PluginBundle.get("window.chat.noMessage"))
        }
        userAvatarButton.addActionListener {
            showLoginDialog()
        }

        add(topToolBar,BorderLayout.NORTH)
        add(chatList,BorderLayout.CENTER)
        add(bottomToolBar,BorderLayout.SOUTH)
    }

    private fun showLoginDialog() {
        JBPopupFactory.getInstance().createComponentPopupBuilder(loginPanel(),toolWindow.component)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .createPopup().showCenteredInCurrentWindow(project)
    }
}