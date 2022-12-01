package shop.itbug.fluttercheckversionx.window

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.fields.ExtendableTextField
import shop.itbug.fluttercheckversionx.dsl.loginPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.UserAccount
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.event.UserLoginStatusEvent
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.BorderLayout
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToolBar

/**
 * Flutter聊天窗口
 */
class FlutterChatMessageWindow(val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()),Disposable {

    private val chatList = JBList<Any>()//聊天显示区域
    private val bottomToolBar = JToolBar()//底部工具栏
    private val chatTextField = ExtendableTextField()//聊天内容编辑框
    private val sendButton = JButton("发送") //发送按钮

    var userInfo: User? = null // 用户信息

    private var userAvatarWidget = object : AnAction("登录","登录典典账号使用更多功能",AllIcons.General.User){
        override fun actionPerformed(e: AnActionEvent) {
            showLoginDialog()
        }
    }

    private var defaultActions = DefaultActionGroup().apply {
        add(userAvatarWidget)
    }
    private val actionToolbar = ActionManager.getInstance().createActionToolbar("ldd-chat-toolbar", defaultActions,false)

    init {
        uiInit()
        userInfo = service<AppService>().user
        ApplicationManager.getApplication().messageBus.connect().subscribe(UserLoginStatusEvent.TOPIC,object : UserLoginStatusEvent{
            override fun loginSuccess(user: User?) {
                userInfo = user
                userInfoHandle()
            }
        })
        userInfoHandle()
    }

    fun userInfoHandle(){
        if(userInfo!=null){
            println("进来了...")
            val  icon = ImageIcon(userInfo!!.picture)
            val ava = JBLabel(icon)
            actionToolbar.component.remove(0)
            actionToolbar.component.add(ava,0)
        }
    }


    private fun uiInit() {

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
        actionToolbar.targetComponent = toolWindow.component
        add(actionToolbar.component, BorderLayout.LINE_START)
        add(chatList, BorderLayout.CENTER)
        add(bottomToolBar, BorderLayout.SOUTH)
    }

    private fun showLoginDialog() {
        JBPopupFactory.getInstance().createComponentPopupBuilder(loginPanel(this) {
            login(it)
        }, toolWindow.component)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .createPopup().showCenteredInCurrentWindow(project)
    }

    private fun login(account: UserAccount) {
        println(account.toString())
    }

    override fun dispose() {

    }
}