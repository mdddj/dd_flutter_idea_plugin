package shop.itbug.fluttercheckversionx.form.components

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.form.socket.MyCustomItemRender
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import javax.swing.DefaultListModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * api列表
 */
class ApiListPanel(val project: Project):JBList<Request>(), ListSelectionListener {

    private fun listModel() : DefaultListModel<Request> = model as DefaultListModel

    init {
        model = DefaultListModel()
        cellRenderer = MyCustomItemRender()
        setNewApiInChangeList()
        setApiListEmptyText()
        addListSelectionListener(this)
    }


    /**
     * 监听到api进入,更新模型
     */
    private fun setNewApiInChangeList(){
        ApplicationManager.getApplication().messageBus.connect().subscribe(SocketMessageBus.CHANGE_ACTION_TOPIC, object :
            SocketMessageBus {
            override fun handleData(data: ProjectSocketService.SocketResponseModel?) {
                data?.let {
                    listModel().addElement(data)
                }
            }
        })
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