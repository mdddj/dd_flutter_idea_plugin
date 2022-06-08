package shop.itbug.fluttercheckversionx.services

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import shop.itbug.fluttercheckversionx.dialog.LoginDialog
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent
import javax.swing.JLabel


///用户面板
class MyUserBarFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "user-account"
    }

    override fun getDisplayName(): String {
        return "典典账号登录"
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return MyUserAccountBar(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}


///底部状态栏的组件
class MyUserAccountBar(var project: Project): CustomStatusBarWidget {
    override fun dispose() {
    }

    override fun ID(): String {
        return "ldd accout"
    }

    override fun install(statusBar: StatusBar) {
    }

    override fun getComponent(): JComponent {
        val jLabel = JLabel(AllIcons.General.User)
        jLabel.addMouseListener(object : MouseListener{
            override fun mouseClicked(e: MouseEvent?) {
                LoginDialog(project = project ).show()
            }

            override fun mousePressed(e: MouseEvent?) {
            }

            override fun mouseReleased(e: MouseEvent?) {
            }

            override fun mouseEntered(e: MouseEvent?) {
            }

            override fun mouseExited(e: MouseEvent?) {
            }

        })
        return jLabel
    }


}