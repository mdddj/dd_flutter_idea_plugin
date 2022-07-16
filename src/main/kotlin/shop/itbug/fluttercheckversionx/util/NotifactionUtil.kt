package shop.itbug.fluttercheckversionx.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

//通知相关工具类
class MyNotifactionUtil {

    companion object {

        //        socket 相关通知
        fun socketNotif(message: String, project: Project,type: NotificationType = NotificationType.INFORMATION) {
            NotificationGroupManager.getInstance().getNotificationGroup("dio_socket_notif")
                .createNotification(message,type)
                .notify(project)
        }
    }
}