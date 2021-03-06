package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.dialog.DioHelpDialog
import shop.itbug.fluttercheckversionx.dialog.RequestDetailPanel
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.form.actions.DioRequestSearch
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.form.actions.StateCodeFilterBox
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.services.SocketMessageBus
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

typealias Request = SocketResponseModel

// 监听http请求的窗口
class SocketRequestForm(val project: Project) : ListSelectionListener { /// 表格模型


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
        LeftActionTools(project,requestsJBList, rightPanel, rightNextPanel, rightFirstPanel) {
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
                val filters = reqs.filter { it.methed.uppercase() == type.uppercase() }
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

        ///jlist初始化
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
        val bottomToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Dio Request")
        toolBar.targetComponent = bottomToolWindow?.component
        leftActionTools.targetComponent = bottomToolWindow?.component

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


        // 接收消息总线传来的对象,并刷新列表
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            SocketMessageBus.CHANGE_ACTION_TOPIC, object : SocketMessageBus {
                override fun handleData(data: SocketResponseModel?) {
                    refreshData(null)
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

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e?.valueIsAdjusting == false) {
            val firstIndex = requestsJBList.selectedIndex
            if (firstIndex < 0) return
            val element = requestsJBList.model.getElementAt(firstIndex)
            if (leftToolBarCore.isInDetailView) {
                leftToolBarCore.changeRequestInDetail(element)
            } else {
                rightFirstPanel.changeShowValue(element)
            }
        }
    }


    private fun addHelpText() {
        requestsJBList.setEmptyText("暂时没有监听到请求.")
        requestsJBList.emptyText.apply {
            appendLine("此功能需要搭配flutter插件使用.")
            appendLine("")
            appendLine(
                AllIcons.Actions.Help, "使用教程?", SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    JBUI.CurrentTheme.Link.Foreground.ENABLED
                )
            ) {
                DioHelpDialog(project).show()
            }
            appendLine("")
            appendText(
                "请梁典典喝咖啡(打赏)", SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    JBUI.CurrentTheme.Link.Foreground.ENABLED
                )
            ) {
                RewardDialog(project).show()
            }
        }
    }

}
