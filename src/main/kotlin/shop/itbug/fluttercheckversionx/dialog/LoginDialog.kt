package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.ui.ComponentValidator
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBFont
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.LocalhostServiceCreate
import shop.itbug.fluttercheckversionx.services.LoginParam
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.event.DocumentEvent

const val borderPaddings = 30

///登录的弹窗
class LoginDialog : JBPanel<LoginDialog>(BorderLayout()) {

    private val usernameTextFiled = ExtendableTextField()
    private val passwordTextFiled = JBPasswordField()
    private val titleLabel = JBLabel("典典账号授权")
    private val loginButton = JButton("登录")
    private val loading = AsyncProcessIcon("登录中")
    private val apiService get() = LocalhostServiceCreate.create(ItbugService::class.java)


    init {
        add(form, BorderLayout.CENTER)
        border = BorderFactory.createEmptyBorder(borderPaddings, borderPaddings, borderPaddings, borderPaddings)
        titleLabel.font = JBFont.h2()
        loginButton.apply {
            addActionListener { login() }
        }
        validateInit()
    }


    /**
     * 验证字符串的输入准确性
     */
    private fun validateInit() {
        usernameTextFiled.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                ComponentValidator.getInstance(usernameTextFiled).ifPresent { v -> v.revalidate() }
            }

        })
    }


    /**
     * 请求服务器的登录接口
     *
     */
    private fun login() {
        try {
            val response =
                apiService.login(LoginParam(usernameTextFiled.text, passwordTextFiled.password.toString())).execute()
            if (response.isSuccessful) {
                val data = response.body()
                data?.let {
                    val token = it.data
                    println("登录成功:${token}")
                }
            } else {
                println("登录失败")
            }
        } catch (e: Exception) {
            println("登录失败:${e.message}")
        }
    }


    private val form
        get() = FormBuilder.createFormBuilder()
            .addComponent(titleLabel)
            .addSeparator(2)
            .addVerticalGap(18)
            .addLabeledComponent("账号", usernameTextFiled, false)
            .addLabeledComponent("密码", passwordTextFiled, false)
            .setVertical(true)
            .addComponent(loginButton)
            .panel

}