package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.StateMachineEnum.NEW_SESSION
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.constance.helpText
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.dsl.docPanel
import shop.itbug.fluttercheckversionx.dsl.requestDetailPanel
import shop.itbug.fluttercheckversionx.dsl.showCenter
import shop.itbug.fluttercheckversionx.form.actions.DioRequestSearch
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.form.actions.StateCodeFilterBox
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.services.event.SocketConnectStatusMessageBus
import shop.itbug.fluttercheckversionx.services.event.SocketMessageBus
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

typealias Request = SocketResponseModel

// 监听http请求的窗口
class SocketRequestForm(val project: Project, private val toolWindow: ToolWindow) : ListSelectionListener { /// 表格模型


    /**
     * 最外层的那个容器
     * idea的分割器
     */
    private var containerJBSplitter = OnePixelSplitter()

    /**
     * idea全部监听到的请求组件
     */
    private var requestsJBList = JBList<Request>()

    private val jbScrollPane = JBScrollPane(requestsJBList)

    /**
     * 右侧面板
     */
    private val rightPanel = JPanel(CardLayout())
    private val rightFirstPanel = RightDetailPanel(project)
    private val rightNextPanel = RequestDetailPanel(project)


    /**
     * 操作工具栏
     */
    private val dioToolbar = JToolBar()

    /**
     * 项目级别的筛选
     */
    private val projectFilterBox = ProjectFilter()

    ///左侧竖行工具栏
    private var leftToolBarCore: LeftActionTools =
        LeftActionTools(project, requestsJBList, rightPanel, rightNextPanel, rightFirstPanel) {
            val datas = (requestsJBList.model as MyDefaultListModel).list
            requestsJBList.model = MyDefaultListModel(datas = datas)
        }

    ///左侧区域操作栏
    private val leftActionTools = LeftActionTools.create(leftToolBarCore)

    /**
     * 状态码级别的筛选 get,post,等等
     */
    private val stateCodeFilterBox = StateCodeFilterBox { type ->
        run {
            val currentProject = projectFilterBox.model.selectedItem.toString()
            val reqs = service.getRequestsWithProjectName(currentProject)
            if (type == "All") {
                requestsJBList.model = MyDefaultListModel(datas = reqs)
            } else {
                //执行过滤
                val filters = reqs.filter { it.method?.uppercase() == type.uppercase() }
                requestsJBList.model = MyDefaultListModel(datas = filters)
            }

        }
    }


    private var searchTextField: DioRequestSearch

    init {
        val initProjects = service.getAllProjectNames()
        if (initProjects.isNotEmpty()) {
            projectFilterBox.change(initProjects)
        }
        if (projectFilterBox.selectedItem != null) {
            requestsJBList.model =
                projectFilterBox.selectedItem?.let { service.getRequestsWithProjectName(it.toString()) }?.let {
                    MyDefaultListModel(datas = it)
                }
        } else {
            requestsJBList.model = MyDefaultListModel(datas = emptyList())
        }
        addHelpText()
        requestsJBList.cellRenderer = MyCustomItemRender()
        requestsJBList.isFocusable = true
        requestsJBList.addListSelectionListener(this)

        ///构建左侧UI
        val leftPanel = JPanel()
        leftPanel.preferredSize = Dimension(400, 0)
        leftPanel.layout = BorderLayout(2, 2)

        jbScrollPane.isOpaque = true // 设置透明度
        jbScrollPane.border = BorderFactory.createEmptyBorder()

        leftPanel.add(jbScrollPane, BorderLayout.CENTER)
        leftPanel.add(leftActionTools.component, BorderLayout.LINE_START)


        // 工具条
        val actionManager = ActionManager.getInstance()
        val toolBar: ActionToolbar = actionManager.createActionToolbar(
            "Dio action Toolbar",
            DefaultActionGroup.EMPTY_GROUP,
            true
        )
        toolBar.targetComponent = toolWindow.component
        leftActionTools.targetComponent = toolWindow.component

        /// 接口搜索过滤
        searchTextField = DioRequestSearch {
            refreshData(it)
        }


        dioToolbar.add(toolBar.component)


        dioToolbar.add(searchTextField)
        dioToolbar.add(projectFilterBox)
        dioToolbar.add(stateCodeFilterBox)

        dioToolbar.isFloatable = false

        leftPanel.add(dioToolbar, BorderLayout.PAGE_START)


        leftPanel.minimumSize = Dimension(350, 0)
        leftPanel.border = BorderFactory.createEmptyBorder() // 清空边框


        ///构建右侧的面板
        rightPanel.border = BorderFactory.createEmptyBorder()

        rightPanel.add(rightFirstPanel, "response_body_panel")
        rightPanel.add(rightNextPanel, "right_detail_panel")


        containerJBSplitter.isOpaque = true
        containerJBSplitter.firstComponent = leftPanel
        containerJBSplitter.secondComponent = rightPanel


        listenData()

    }

    private val messageBus get() = ApplicationManager.getApplication().messageBus


    private fun listenData() {
        // api消息进入
        messageBus.connect().subscribe(
            SocketMessageBus.CHANGE_ACTION_TOPIC, object : SocketMessageBus {
                override fun handleData(data: SocketResponseModel?) {
                    val setting = PluginStateService.getInstance().state
                    setting?.apply {
                        data?.let {
                            MyNotificationUtil.toolWindowShowMessage(project, data.url ?: "")
                        }.takeIf { this.apiInToolwindowTop && data?.url != null }
                    }
                    refreshData(null)
                }
            }
        )
        // socket连接状态变更
        messageBus.connect().subscribe(
            SocketConnectStatusMessageBus.CHANGE_ACTION_TOPIC, object : SocketConnectStatusMessageBus {
                override fun statusChange(aioSession: AioSession?, stateMachineEnum: StateMachineEnum?) {
                    when (stateMachineEnum) {
                        NEW_SESSION -> {
                            print("新连接")
                        }
                        else -> {
                            println("socket状态变更:$stateMachineEnum")
                        }
                    }
                }
            }
        )

    }


    /**
     * 滚动到底部
     */
    private fun autoScrollToBottom() {
        jbScrollPane.verticalScrollBar.value = jbScrollPane.verticalScrollBar.maximum + 20
    }


    fun getContent(): JComponent {
        return containerJBSplitter
    }

    private val service get() = service<AppService>()

    /**
     * 刷新列表的数据
     */
    private fun refreshData(list: List<Request>?) {
        SwingUtilities.invokeLater {
            if (list == null) {
                val allRequest = service.getAllRequest()
                requestsJBList.model = MyDefaultListModel(datas = allRequest)
                if (allRequest.isEmpty()) {
                    rightFirstPanel.clean()
                }
            } else {
                requestsJBList.model = MyDefaultListModel(datas = list)
            }
            val allProjectNames = service.getAllProjectNames()
            projectFilterBox.change(allProjectNames)

            //自动滚动到最底部
            if (leftToolBarCore.isSelect()) {
                autoScrollToBottom()
            }
        }
    }

    //切换接口详情显示
    override fun valueChanged(e: ListSelectionEvent?) {
        if (e?.valueIsAdjusting == false) {
            val firstIndex = requestsJBList.selectedIndex
            if (firstIndex < 0) return
            val element = requestsJBList.model.getElementAt(firstIndex)
            if (leftToolBarCore.isInDetailView) {
                leftToolBarCore.changeRequestInDetail(element)

                val content = ContentFactory.SERVICE.getInstance()
                    .createContent(JBScrollPane(requestDetailPanel(element, project)), "API", false).apply {
                        isCloseable = true
                        icon = AllIcons.Actions.Close
                    }

                toolWindow.contentManager.addContent(content)
                toolWindow.contentManager.setSelectedContent(content)
            } else {
                rightFirstPanel.changeShowValue(element)
            }
        }
    }


    ///添加帮助性文档
    private fun addHelpText() {
        requestsJBList.setEmptyText(PluginBundle.get("empty"))
        requestsJBList.emptyText.apply {
            appendLine(
                PluginBundle.get("help"), SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    JBUI.CurrentTheme.Link.Foreground.ENABLED
                )
            ) {
                docPanel(helpText, project).showCenter(project)
            }
            appendText(" ")
            appendText(PluginBundle.get("reward"),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED,JBUI.CurrentTheme.Link.Foreground.ENABLED)
            ){
                RewardDialog(project).show()
            }
        }
    }

}
