package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.services.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


class LoginDialog: JPanel(BorderLayout()) {

    private val usernameTextFiled = ExtendableTextField()
    private val passwordTextFiled = ExtendableTextField()
    private val loginButton = JButton("登录")

    init {
        add(form,BorderLayout.CENTER)
        border = BorderFactory.createEmptyBorder(12,12,12,12)
    }


    private val form get() = FormBuilder.createFormBuilder()
        .addLabeledComponent("账号",usernameTextFiled,true)
        .addLabeledComponent("密码",passwordTextFiled,true)
        .addComponentToRightColumn(loginButton)
        .addComponentToRightColumn(JLabel("注册"))
        .panel

}