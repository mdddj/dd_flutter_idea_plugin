package shop.itbug.fluttercheckversionx.util

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.dialog.LoginDialogV2
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.i18n
import shop.itbug.fluttercheckversionx.icons.MyIcons
import javax.swing.JComponent

//通知相关工具类
class MyNotificationUtil {

    companion object {
        const val toolWindowId = "FlutterX"

        //socket 相关通知
        fun socketNotify(message: String, project: Project, type: NotificationType = NotificationType.INFORMATION) {
            NotificationGroupManager.getInstance().getNotificationGroup("dio_socket_notify")
                .createNotification(message, type).apply {
                    icon = MyIcons.flutter
                }
                .addAction(object : MyDumbAwareAction({ "bug-feedback".i18n() }) {
                    override fun actionPerformed(e: AnActionEvent) {
                        message.copyTextToClipboard()
                        BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
                    }
                })
                .notify(project)
        }

        /**
         * 显示tool window 弹窗
         */
        fun toolWindowShowMessage(project: Project, message: String, type: MessageType = MessageType.INFO) {
            ToolWindowManager.getInstance(project)
                .notifyByBalloon(toolWindowId, type, "<html><p>$message</p></html>")
        }

        fun tooWindowShowMessage(project: Project, com: JComponent) {
            val createBalloon = JBPopupFactory.getInstance().createBalloonBuilder(com).createBalloon()
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId)
            toolWindow?.apply {
                createBalloon.showInCenterOf(toolWindow.component)
            }
        }


        ///弹出登录的窗口
        fun showLoginDialog(project: Project) {
//            JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(loginPanel(parentDisposable), preferableFocusComponent)
//                .setMovable(true)
//                .setRequestFocus(true)
//                .setFocusable(true)
//                .createPopup().showCenteredInCurrentWindow(project)
            LoginDialogV2(project).show()
        }

    }
}