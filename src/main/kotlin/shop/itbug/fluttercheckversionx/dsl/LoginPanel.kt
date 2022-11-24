package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

///登录弹窗
fun loginPanel() : DialogPanel {
    return panel {
        row (PluginBundle.get("account.text")){
            textField()
        }
        row (PluginBundle.get("password.text")) {
            cell(JBPasswordField()).horizontalAlign(HorizontalAlign.FILL)
        }
        row {
            button(PluginBundle.get("window.chat.loginDialog.text")) {}
                .gap(RightGap.SMALL)
            comment(PluginBundle.get("window.chat.loginDialog.register.comment"))
        }
    }
}