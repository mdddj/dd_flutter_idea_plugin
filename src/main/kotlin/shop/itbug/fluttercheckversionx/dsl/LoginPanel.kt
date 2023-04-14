package shop.itbug.fluttercheckversionx.dsl

import cn.hutool.core.lang.Validator
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.JBFont
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.UserAccount
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.LoginParam
import shop.itbug.fluttercheckversionx.services.SERVICE

///登录弹窗
fun loginPanel(parentDisposable: Disposable, account: UserAccount): DialogPanel {
    lateinit var panel: DialogPanel

    val passwordJBTextField = JBPasswordField()
    var errorMsg = ""
    fun userLogin() {

        val r =  SERVICE.create(ItbugService::class.java).login(LoginParam(account.username,account.password)).execute().body()



        println("登录结果:$r")

        if(r?.state == 200) {
             r.data?.let {

                 println("进来了..")
            }
        }else{
            errorMsg = r?.message ?: ""
            panel.reset()
        }
    }
    panel = panel {
        row {
            label(PluginBundle.get("window.chat.loginDialog.title")).component.font = JBFont.h2().asBold()
        }.bottomGap(BottomGap.MEDIUM)
        row(PluginBundle.get("account.text")) {
            textField().bindText(account::username).validationOnInput { v ->
                val isEmail = Validator.isEmail(v.text)
                if (isEmail) null else ValidationInfoBuilder(v).error("请输入邮箱")
            }
        }
        row(PluginBundle.get("password.text")) {
            cell(passwordJBTextField).align(Align.FILL).validationOnInput {
                val pass = String(it.password)
                if (pass.length < 6 || pass.length > 20) ValidationInfo("密码字符6-20长度") else null
            }
        }
        row {
            checkBox(PluginBundle.get("window.idea.loginDialog.remember")).enabled(true).align(Align.FILL)
        }.topGap(TopGap.SMALL)
        row {
            comment(PluginBundle.get("window.chat.loginDialog.register.comment"))
            button(PluginBundle.get("window.chat.loginDialog.text")) {
                account.password = String(passwordJBTextField.password)
                panel.apply()
                userLogin()
            }.gap(RightGap.SMALL).align(Align.FILL)
        }.topGap(TopGap.SMALL)
        row {
            label(errorMsg)
        }
    }.addBorder()
    val newDisposable = Disposer.newDisposable()
    panel.registerValidators(newDisposable)
    Disposer.register(parentDisposable, newDisposable)
    return panel
}
