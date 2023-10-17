package shop.itbug.fluttercheckversionx.window

import cn.hutool.db.Entity
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import note.jdbc.FlutterCollectService
import shop.itbug.fluttercheckversionx.bus.FlutterPluginCollectEvent
import shop.itbug.fluttercheckversionx.bus.FlutterPluginCollectEventType
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.dialog.MyPluginAddToPubspecFileDialog
import java.awt.Component
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.ListCellRenderer

class DartPluginsWindow(private val toolwindow: ToolWindow, val project: Project) : BorderLayoutPanel() {

    val list = DartPluginList(project)

    fun isSelect(): Boolean = list.selectedIndex >= 0

    private val toolbar =
        ActionManager.getInstance().createActionToolbar("dart-plugin-toolwindow", createActions().apply {
        }, true).apply {
            targetComponent = toolwindow.component
        }

    ///操作
    private fun createActions(): DefaultActionGroup = DefaultActionGroup().apply {


        //刷新列表
        add(object : MyDumbAwareAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                list.refresh()
            }
        })

        //删除
        add(object : MyDumbAwareAction("Remove", "Remove Collection", AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                //删除错误
                list.remove()
                DaemonCodeAnalyzer.getInstance(project).restart()
            }

            override fun update(e: AnActionEvent) {
                super.update(e)
                e.presentation.isEnabled = isSelect()
            }
        })

        //添加到依赖文件
        add(object : MyDumbAwareAction("Add to pubspec.yaml file", "", AllIcons.Actions.AddList) {
            override fun actionPerformed(e: AnActionEvent) {
                MyPluginAddToPubspecFileDialog(project, list.selectedValue).show()
            }

            override fun update(e: AnActionEvent) {
                super.update(e)
                e.presentation.isEnabled = isSelect()
            }
        })
    }

    init {
        addToCenter(JBScrollPane(list).apply { border = null })
        addToTop(toolbar.component)
    }

}


class DartPluginList(val project: Project) : JBList<Entity>() {
    private fun all(): List<Entity> = FlutterCollectService.selectAll()

    init {
        model = DefaultListModel<Entity>().apply {
            addAll(all())
        }
        cellRenderer = DartPluginNameCellRender()

        FlutterPluginCollectEvent.listen { type, _ ->
            when (type) {
                FlutterPluginCollectEventType.add -> refresh()
                FlutterPluginCollectEventType.remove -> refresh()
            }
        }
        border = null
    }


    fun refresh() {
        (model as DefaultListModel).apply {
            clear()
            addAll(all())
        }
    }

    ///删除某个项目
    fun remove() {
        if (selectedIndex >= 0) {
            //删除选中
            val remove = FlutterCollectService.remove(selectedValue.getStr("name"))
            DaemonCodeAnalyzer.getInstance(project).restart()
            if (remove && selectedIndex >= 0) {
                (model as DefaultListModel).remove(selectedIndex)
            }
        }
    }
}


///行渲染
class DartPluginNameCellRender : JBLabel(), ListCellRenderer<Entity> {

    override fun getListCellRendererComponent(
        list: JList<out Entity>?,
        value: Entity?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        text = value?.getStr("name")
        return this
    }


}