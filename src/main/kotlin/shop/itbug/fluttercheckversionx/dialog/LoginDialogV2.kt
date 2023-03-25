package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.UserAccount
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.*
import shop.itbug.fluttercheckversionx.services.event.UserLoginStatusEvent
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.CredentialUtil
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class LoginDialogV2(project: Project): MyDialogWrapper(project),Disposable {

    private val account = UserAccount()
    lateinit var ui: DialogPanel
    init {
        super.init()
        title = "账号登录"
        setOKButtonText(PluginBundle.get("window.chat.loginDialog.text"))
        isResizable=false

    }

    override fun createCenterPanel(): JComponent {
        ui = panel {
            row {
                textField().bindText(account::username)
            }
            row {
                passwordField().bindText(account::password)
            }
        }

        return ui
    }

    //登录成功
    private fun loginSuccess(user: User) {

        println("success.....$user")
        AppService.getInstance().user = user
        UserLoginStatusEvent.fire(user)
        project.toast("Success: ${user.nickName}")
        super.doCancelAction()
    }


    override fun doOKAction() {
        ui.apply()
        doLogin()
    }



    private fun doLogin() {
        SERVICE.create(ItbugService::class.java).login(LoginParam(account.username,account.password)).enqueue(object :
            Callback<JSONResult<LoginResult?>> {
            override fun onResponse(
                call: Call<JSONResult<LoginResult?>>,
                response: Response<JSONResult<LoginResult?>>
            ) {

                response.body()?.apply {

                    if(state == 200 && data != null){
                        CredentialUtil.saveToken(data.token)
                        loginSuccess(data.user)
                    }else{
                        project.toastWithError(message)
                    }

                }

            }

            override fun onFailure(call: Call<JSONResult<LoginResult?>>, t: Throwable) {
                project.toastWithError("login error: $t")
            }

        })
    }

    override fun dispose() {
        super.dispose()
    }

    override fun doValidate(): ValidationInfo {
        ui.apply()
        if(account.username.isEmpty()){
            return ValidationInfo("请输入用户名")
        }else if(account.password.isEmpty()){
            return  ValidationInfo("请输入密码")
        }
        return  ValidationInfo("").withOKEnabled()
    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        return super.createButtonsPanel(buttons)
    }



}