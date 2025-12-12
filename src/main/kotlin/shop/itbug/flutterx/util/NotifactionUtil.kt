package shop.itbug.flutterx.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import shop.itbug.flutterx.icons.MyIcons

//通知相关工具类
class MyNotificationUtil {

    companion object {

        //socket 相关通知
        fun socketNotify(message: String, project: Project, type: NotificationType = NotificationType.INFORMATION) {
            NotificationGroupManager.getInstance().getNotificationGroup("dio_socket_notify")
                .createNotification(message, type).apply {
                    icon = MyIcons.flutter
                }
//                .addAction(object : MyDumbAwareAction({ "bug-feedback".i18n() }) {
//                    override fun actionPerformed(e: AnActionEvent) {
//                        message.copyTextToClipboard()
//                        BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
//                    }
//                })
                .notify(project)
        }


    }
}