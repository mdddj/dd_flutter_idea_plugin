package shop.itbug.fluttercheckversionx.util

import com.intellij.codeInsight.hint.HintManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComponent

//通知相关工具类
class MyNotificationUtil {

    companion object {
        private const val toolWindowId = "Dio Request"

        //        socket 相关通知
        fun socketNotif(message: String, project: Project, type: NotificationType = NotificationType.INFORMATION) {
            NotificationGroupManager.getInstance().getNotificationGroup("dio_socket_notif")
                .createNotification(message, type)
                .notify(project)
        }

        /**
         * 显示tool window 弹窗
         */
        fun toolWindowShowMessage(project: Project, message: String,type: MessageType = MessageType.INFO) {
            ToolWindowManager.getInstance(project)
                .notifyByBalloon(toolWindowId, type, "<html><p>$message</p></html>")
        }

        fun tooWindowShowMessage(project: Project,com: JComponent) {
            val createBalloon = JBPopupFactory.getInstance().createBalloonBuilder(com).createBalloon()
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId)
            toolWindow?.apply {
                // todo
//                HintManager.getInstance().showHint(com, RelativePoint(component.locationOnScreen),1,1000)
//                createBalloon.show(component.rootPane.layeredPane)
            }
        }
    }
}