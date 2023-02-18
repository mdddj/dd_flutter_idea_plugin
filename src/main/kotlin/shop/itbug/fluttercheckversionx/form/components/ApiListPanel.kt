package shop.itbug.fluttercheckversionx.form.components

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.form.socket.MyCustomItemRender
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.projectClosed
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * api列表
 */
class ApiListPanel(val project: Project) : JBList<Request>(), ListSelectionListener {

    private fun listModel(): DefaultListModel<Request> = model as DefaultListModel

    init {
        model = DefaultListModel()
        cellRenderer = MyCustomItemRender()
        setNewApiInChangeList()
        setApiListEmptyText()
        addListSelectionListener(this)
        addListening()
    }


    private fun addListening() {
        SwingUtilities.invokeLater {
            val runnable = Runnable { projectChange() }
            service<AppService>().addListening(runnable)
            project.projectClosed { service<AppService>().removeListening(runnable) }
        }
    }

    /**
     * 项目被切换事件
     */
    private fun projectChange() {
        val appService = service<AppService>()
        val projectName = appService.currentSelectName.get()
        projectName?.let {
            val apis = appService.getRequestsWithProjectName(projectName)
            changeApis(apis)
        }
    }

    /**
     * 更新api列表
     */
    private fun changeApis(apis: List<Request>) {
        listModel().apply {
            clear()
            addAll(apis)
        }
    }

    /**
     * 监听到api进入,更新模型
     */
    private fun setNewApiInChangeList() {
        val appService = service<AppService>()
        SocketMessageBus.listening {
            val currentProjectName = appService.currentSelectName.get()
            //如果没有选中项目, 或者当前选中项目等于进入api的项目,才被加进列表中
            if(currentProjectName == null || currentProjectName == it.projectName ) {
                listModel().addElement(it)
            }
        }
    }


    ///添加帮助性文档
    private fun setApiListEmptyText() {
        setEmptyText(PluginBundle.get("empty"))
        emptyText.apply {
            appendLine(
                PluginBundle.get("help"), SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    JBUI.CurrentTheme.Link.Foreground.ENABLED
                )
            ) {
                BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/dio.md")
            }
            appendText(" 丨 ")
            appendText(
                PluginBundle.get("reward"),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED, JBUI.CurrentTheme.Link.Foreground.ENABLED)
            ) {
                RewardDialog(project).show()
            }
            appendText(" 丨 ")
            appendText(
                PluginBundle.get("bugs"),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED, JBUI.CurrentTheme.Link.Foreground.ENABLED)
            ) {
                BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
            }
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {

    }

}