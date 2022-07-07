package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.icons.AllIcons
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer


/**
 * 过滤项目
 * 因为可能会多开多个项目,所以要支持过滤
 * 当然socket也根据项目分离Request请求
 */
class ProjectFilter : JComboBox<String>() {


    init {
        change(emptyList())
        setRenderer(MyCustomListRender())
    }


    /**
     * 改变数据
     */
    fun change(list: List<String>) {
        model = MyJComboBoxModel(list)
        model.selectedItem = current
        if (list.isEmpty()) {
            isEnabled = false
            model.selectedItem = "暂无"
        } else {
            isEnabled = true
        }
        if (list.size == 1) {
            model.selectedItem = list.first()
        }
    }

    private val current: String? = model
        .selectedItem as String?

}

class MyJComboBoxModel(allProjects: List<String>) : DefaultComboBoxModel<String>() {
    private var projects: List<String>

    init {
        projects = allProjects
        removeAllElements()
        addAll(projects)
    }
}

/**
 * 自定义渲染小部件
 */
class MyCustomListRender : ListCellRenderer<String> {

    override fun getListCellRendererComponent(
        list: JList<out String>?,
        value: String?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(JLabel(AllIcons.Actions.ProjectDirectory))
        panel.add(JLabel(value))
        return panel
    }

}

