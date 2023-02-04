package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import icons.MyImages
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import java.awt.BorderLayout
import javax.swing.ImageIcon
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
        println(MyImages.wx2)
        return object : JPanel(BorderLayout()){
            init {
                add(JBLabel().apply {
                    icon = ImageIcon(MyImages.wx2)
                    setSize(400,400)
                },BorderLayout.CENTER)
            }
        }

    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        buttons.removeIf { it.actionCommand =="Cancel" }
        return super.createButtonsPanel(buttons)
    }

}