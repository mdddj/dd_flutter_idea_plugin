package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import shop.itbug.fluttercheckversionx.icons.MyImages
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * 打赏的弹窗
 */
class RewardDialog(var project: Project) : DialogWrapper(project) {
    private var view = MarkdownShowComponent( "加载中...",project)

    init {
        init()
        setSize(400,400)
        title = "打赏"
        setOKButtonText("谢谢支持")
    }


    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                JLabel(MyImages.wx)
            }
            row {
                MyImages.wx
            }
        }

    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        buttons.removeIf { it.actionCommand =="Cancel" }
        return super.createButtonsPanel(buttons)
    }

}