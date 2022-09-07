package shop.itbug.fluttercheckversionx.setting

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class MySettingPanel: JPanel() {

    private val scroll = JBScrollPane()
    init {
        layout = BorderLayout()
        add(SocketPortSettingPanel(),BorderLayout.CENTER)
    }
}

class SocketPortSettingPanel: JPanel(){

    private var label1 = JBLabel("Dio soket listening port\n")
    private var textField = JBTextField("")
    private val button = JButton("确定并重新启动")
    init {
        layout = BoxLayout(this,BoxLayout.X_AXIS)
        add(label1)
        add(textField)
        add(button)
    }
}
