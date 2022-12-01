package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import shop.itbug.fluttercheckversionx.dsl.loginPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import java.awt.Insets
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class LoginDialogV2 (project: Project): DialogWrapper(project),Disposable {


    init {
        super.init()
        title = "账号登录"
        setOKButtonText(PluginBundle.get("window.chat.loginDialog.text"))
        isResizable=false

    }

    override fun createCenterPanel(): JComponent {
        return loginPanel(this) {
            println("登录$it")
            super.doValidate()
        }
    }

    override fun dispose() {
        super.dispose()
    }

    override fun doValidate(): ValidationInfo {
        return ValidationInfo("登录失败")
    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        buttons.removeIf { it.actionCommand == "Cancel" }
        return super.createButtonsPanel(buttons)
    }




}