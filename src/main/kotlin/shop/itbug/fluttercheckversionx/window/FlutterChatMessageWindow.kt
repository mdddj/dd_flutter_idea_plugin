package shop.itbug.fluttercheckversionx.window

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.fields.ExtendableTextField
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dsl.changeRoomPanel
import shop.itbug.fluttercheckversionx.dsl.show
import shop.itbug.fluttercheckversionx.dsl.userDetailSimplePanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.Pageable
import shop.itbug.fluttercheckversionx.model.chat.IdeaMessage
import shop.itbug.fluttercheckversionx.model.chat.SendTextModel
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.render.ChatHistoryListModel
import shop.itbug.fluttercheckversionx.render.ChatMessageRender
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.event.UserLoginStatusEvent
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import java.awt.BorderLayout
import java.net.URL
import javax.swing.*

/**
 * Flutter聊天窗口
 */
class FlutterChatMessageWindow(val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()),
    Disposable {

    private val chatList = JBList<IdeaMessage>()//聊天显示区域
    private val bottomToolBar = JToolBar()//底部工具栏
    private val chatTextField = ExtendableTextField()//聊天内容编辑框
    private val sendButton = JButton("发送") //发送按钮
    private val chatListModel = ChatHistoryListModel(emptyList())
    private val appService get() = service<AppService>()

    var userInfo: User? = appService.user // 用户信息

    //用户信息&用户登录
    private var userAvatarWidget = object : MyAction("登录", "登录典典账号使用更多功能", AllIcons.General.User) {
        override fun actionPerformed(e: AnActionEvent) {
            if (userInfo == null) {
                showLoginDialog()
            } else {
                e.inputEvent?.let {
                    JBPopupFactory.getInstance().createComponentPopupBuilder(userDetailSimplePanel(userInfo!!), null)
                        .createPopup().show(RelativePoint(it.component.locationOnScreen))
                }

            }

        }
    }

    //切换房间列表功能
    private val chatRoomsWidget =
        object : MyAction("切换房间", "切换聊天房间", AllIcons.Toolwindows.ToolWindowMessages) {
            override fun actionPerformed(e: AnActionEvent) {
                changeRoomPanel {
                    loadRoomsHistory()
                }.show(e)
            }

        }

    private var defaultActions = DefaultActionGroup().apply {
        add(userAvatarWidget)
        add(chatRoomsWidget)
    }
    private val actionToolbar =
        ActionManager.getInstance().createActionToolbar("ldd-chat-toolbar", defaultActions, false)

    init {
        uiInit()
        userInfo = service<AppService>().user
        UserLoginStatusEvent.listening {
            userInfo = it
            userInfoHandle()
        }
        userInfoHandle()

        //加载历史消息记录
        loadRoomsHistory()
    }


    fun userInfoHandle() {
        userInfo?.apply {
            val icon = ImageIcon(URL(picture),nickName)
            val ava = JBLabel(icon)
            actionToolbar.component.remove(0)
            actionToolbar.component.add(ava, 0)
        }
    }


    /**
     * 发送一条文本消息
     */
    private fun send() {
        val charRoom = service<AppService>().currentChatRoom
        charRoom?.let {
            val result =
                SERVICE.create<ItbugService>().sendSimpleMessage(SendTextModel(chatTextField.text, it.id)).execute()
                    .body()
            result?.apply {
                if (state != 200) {
                    MyNotificationUtil.socketNotify(message, project, NotificationType.ERROR)
                }
            }
        }.takeIf { userInfo != null }
    }


    /**
     * ui组件初始化
     */
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
            cellRenderer = ChatMessageRender()
            model = chatListModel
            border = BorderFactory.createEmptyBorder(12,12,12,12)
        }
        sendButton.addActionListener {
            send()
        }
        actionToolbar.targetComponent = toolWindow.component
        add(actionToolbar.component, BorderLayout.LINE_START)
        add(JBScrollPane(chatList), BorderLayout.CENTER)
        add(bottomToolBar, BorderLayout.SOUTH)
    }

    //登录弹窗
    private fun showLoginDialog() {
        MyNotificationUtil.showLoginDialog(project)
    }


    //加载历史的聊天列表
    private fun loadRoomsHistory() {
        appService.currentChatRoom?.let {
            val findRoomHistory = SERVICE.create<ItbugService>().findRoomHistory(it.id, 0, 10)
            findRoomHistory.enqueue(object : Callback<JSONResult<Pageable<IdeaMessage>>> {
                override fun onResponse(
                    call: Call<JSONResult<Pageable<IdeaMessage>>>,
                    response: Response<JSONResult<Pageable<IdeaMessage>>>
                ) {
                    response.body()?.data?.apply {
                        println("加载到消息条数:${this.content.size}")
                        chatList.model = ChatHistoryListModel(this.content)
                    }
                }

                override fun onFailure(call: Call<JSONResult<Pageable<IdeaMessage>>>, t: Throwable) {
                    t.printStackTrace()
                }

            })
        }

        if (appService.currentChatRoom == null) {
            println("没有选择房间,无法加载")
        }

    }

    override fun dispose() {

    }


}