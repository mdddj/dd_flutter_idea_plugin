package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Flutter聊天窗口
 */
class FlutterChatMessageWindow(val project: Project) : JPanel(BorderLayout()) {


    private val topMessageLabel = JLabel("开发中,敬请期待,意见加QQ群:706438100")

    init {
        compentInit()
    }


    /**
     * 初始化组件
     */
    private fun compentInit() {
        add(topMessageLabel,BorderLayout.NORTH)
    }
}