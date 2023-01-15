package shop.itbug.fluttercheckversionx.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import shop.itbug.fluttercheckversionx.dsl.loginPanel
import javax.swing.JComponent

//通知相关工具类
class MyNotificationUtil {

    companion object {
        private const val toolWindowId = "Dio Request"

        //        socket 相关通知
        fun socketNotify(message: String, project: Project, type: NotificationType = NotificationType.INFORMATION) {
            NotificationGroupManager.getInstance().getNotificationGroup("dio_socket_notif")
                .createNotification(message, type)
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
                println("弹窗")
                createBalloon.showInCenterOf(toolWindow.component)
            }
        }

        fun showLoginDialog(project: Project, preferableFocusComponent: JComponent, parentDisposable: Disposable) {
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(loginPanel(parentDisposable), preferableFocusComponent)
                .setMovable(true)
                .setRequestFocus(true)
                .setFocusable(true)
                .createPopup().showCenteredInCurrentWindow(project)
        }
    }
}