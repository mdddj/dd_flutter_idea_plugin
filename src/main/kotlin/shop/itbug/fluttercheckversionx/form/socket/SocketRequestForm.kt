package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBEmptyBorder
import shop.itbug.fluttercheckversionx.form.components.DioTableToolbar
import shop.itbug.fluttercheckversionx.form.components.RightDetailPanel
import shop.itbug.fluttercheckversionx.services.SokcetMessageBus
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
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
    private var containerJBSplitter = JBSplitter()


    /**
     * idea全部监听到的请求组件
     */
    private var requestsJBList = JBList<Request>()


    /**
     * 右侧面板
     */
    private val rightPanel = RightDetailPanel()



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
        jbScrollPane.viewportBorder = null

        leftPanel.add(jbScrollPane, BorderLayout.CENTER)
        leftPanel.add(geToolBar().component, BorderLayout.PAGE_START)
        leftPanel.border = JBEmptyBorder(1, 1, 1, 1)


        val leftScrollPanel = JBScrollPane(leftPanel)
        leftScrollPanel.minimumSize = Dimension(350, 0)
        leftScrollPanel.viewportBorder = null



        ///构建右侧的面板



        containerJBSplitter.firstComponent = leftScrollPanel
        containerJBSplitter.secondComponent = JBScrollPane(rightPanel)
        containerJBSplitter.border = JBEmptyBorder(2, 2, 2, 2)


        // 接收消息总线传来的对象,并刷新列表
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            SokcetMessageBus.CHANGE_ACTION_TOPIC,
            SokcetMessageBus { refreshData() })

    }


    private fun geToolBar(): ActionToolbar {
        val toolBar = DioTableToolbar.create(
            clean = {
                cleanData()
            },
            projectfilter = {
                println("筛选了项目:$it")
            }
        )
        return toolBar
    }


    fun getContent(): JComponent {
        return containerJBSplitter
    }


    /**
     * 清空表格数据
     */
    private fun cleanData() {
        SwingUtilities.invokeLater {
            service<AppService>().cleanAllRequest()
            requestsJBList.model = MyDefaultListModel(datas = emptyList())
        }
    }

    /**
     * 刷新列表的数据
     */
    private fun refreshData() {
        SwingUtilities.invokeLater {
            val service = service<AppService>()
            val allRequest = service.getAllRequest()
            requestsJBList.model = MyDefaultListModel(datas = allRequest)
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e?.valueIsAdjusting == false) {
            val firstIndex = requestsJBList.selectedIndex
            val element = requestsJBList.model.getElementAt(firstIndex)
            rightPanel.changeShowValue(element,project)
        }


    }

}
