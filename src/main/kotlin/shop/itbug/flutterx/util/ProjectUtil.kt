package shop.itbug.flutterx.util

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun Project.toast(message: String) {
    MyNotificationUtil.socketNotify(message,this)
}
fun Project.toastWithError(message: String) {
    MyNotificationUtil.socketNotify(message,this,NotificationType.ERROR)
}