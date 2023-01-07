package shop.itbug.fluttercheckversionx.util

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun Project.toast(message: String) {
    MyNotificationUtil.socketNotif(message,this)
}
fun Project.toastWithError(message: String) {
    MyNotificationUtil.socketNotif(message,this,NotificationType.ERROR)
}