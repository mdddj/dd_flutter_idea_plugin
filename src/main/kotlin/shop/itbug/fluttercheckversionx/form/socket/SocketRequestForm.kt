package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.form.actions.DioRequestSearch
import shop.itbug.fluttercheckversionx.form.actions.ProjectFilter
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.services.SocketMessageBus
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JToolBar
import javax.swing.SwingUtilities
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


    /**
     * 右侧面板
     */
    private val rightPanel = RightDetailPanel()


    /**
     * 操作工具栏
     */
    private val dioToolbar = JToolBar()


    private val projectFilterBox = ProjectFilter()

    private val searchTextField = DioRequestSearch()

    init {


        ///jlist初始化
        requestsJBList.model = MyDefaultListModel(datas = emptyList())
        requestsJBList.cellRenderer = MyCustomItemRender()
        requestsJBList.isFocusable = true
        requestsJBList.addListSelectionListener(this)

        ///构建左侧UI
        val leftPanel = JPanel()
        leftPanel.preferredSize = Dimension(400, 0)
        leftPanel.layout = BorderLayout(2, 2)
        val jbScrollPane = JBScrollPane(requestsJBList)
        jbScrollPane.isOpaque = true // 设置透明度
        jbScrollPane.border = BorderFactory.createEmptyBorder()

        leftPanel.add(jbScrollPane, BorderLayout.CENTER)


        // 工具条
        val actionManager = ActionManager.getInstance()
        val toolBar = actionManager.createActionToolbar(
            "Dio action Toolbar",
            actionManager.getAction("DioTool.CleanService") as DefaultActionGroup,
            true
        )


        dioToolbar.add(toolBar.component)


        dioToolbar.add(searchTextField)
        dioToolbar.add(projectFilterBox)

        dioToolbar.isFloatable = false

        leftPanel.add(dioToolbar, BorderLayout.PAGE_START)


        leftPanel.minimumSize = Dimension(350, 0)
        leftPanel.border = BorderFactory.createEmptyBorder() // 清空边框


        ///构建右侧的面板


        containerJBSplitter.isOpaque = true
        containerJBSplitter.firstComponent = leftPanel
        containerJBSplitter.secondComponent = rightPanel


        // 接收消息总线传来的对象,并刷新列表
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            SocketMessageBus.CHANGE_ACTION_TOPIC, object : SocketMessageBus {
                override fun handleData(data: SocketResponseModel?) {
                    refreshData()
                }

            }
        )

    }


    fun getContent(): JComponent {
        return containerJBSplitter
    }

    /**
     * 刷新列表的数据
     */
    private fun refreshData() {
        SwingUtilities.invokeLater {
            val service = service<AppService>()
            val allRequest = service.getAllRequest()
            requestsJBList.model = MyDefaultListModel(datas = allRequest)
            if (allRequest.isEmpty()) {
                rightPanel.clean()
            }


            val allProjectNames = service.getAllProjectNames()
            projectFilterBox.change(allProjectNames)


        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e?.valueIsAdjusting == false) {
            val firstIndex = requestsJBList.selectedIndex
            if (firstIndex < 0) return
            val element = requestsJBList.model.getElementAt(firstIndex)
            rightPanel.changeShowValue(element, project)
        }


    }

}
