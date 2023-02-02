package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import icons.MyImages
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import javax.swing.JButton
import javax.swing.JComponent
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
                JBLabel(MyImages.wx)
            }
        }

    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        buttons.removeIf { it.actionCommand =="Cancel" }
        return super.createButtonsPanel(buttons)
    }

}