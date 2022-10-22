package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.FormBuilder
import org.slf4j.LoggerFactory
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.LocalhostServiceCreate
import shop.itbug.fluttercheckversionx.services.LoginParam
import java.awt.BorderLayout
import java.awt.Font
import java.util.function.Supplier
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
const val borderPaddings = 30
///登录的弹窗
class LoginDialog: JPanel(BorderLayout()) {

    private val log = LoggerFactory.getLogger(LoginDialog::class.java)
    private val usernameTextFiled = ExtendableTextField()
    private val passwordTextFiled = JBPasswordField()
    private val titleLabel = JBLabel("典典账号授权")
    private val loginButton = JButton("登录")
    private val apiService get() = LocalhostServiceCreate.create(ItbugService::class.java)


    init {
        add(form,BorderLayout.CENTER)
        border = BorderFactory.createEmptyBorder(borderPaddings,borderPaddings,borderPaddings,borderPaddings)
        titleLabel.font = Font("宋体",Font.PLAIN,23)
        loginButton.apply {
            addActionListener { login() }
        }
        validateInit()
    }


    /**
     * 验证字符串的输入准备性
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
     */
    private fun login() {
       try{
           val response = apiService.login(LoginParam(usernameTextFiled.text,passwordTextFiled.password.toString())).execute()
           if(response.isSuccessful){
                val data = response.body()
               data?.let {
                   val token = it.data
                   println("登录成功:${token}")
               }
           }else{
               println("登录失败")
           }
       }catch (e:Exception){
           println("登录失败:${e.message}")
       }
    }





    private val form get() = FormBuilder.createFormBuilder()
        .addComponent(titleLabel)
        .addSeparator()
        .addVerticalGap(18)
        .addLabeledComponent("账号",usernameTextFiled,false)
        .addLabeledComponent("密码",passwordTextFiled,false)
        .addComponent(loginButton)
        .panel

}