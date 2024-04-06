package shop.itbug.fluttercheckversionx.util

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.i18n
import shop.itbug.fluttercheckversionx.icons.MyIcons

//通知相关工具类
class MyNotificationUtil {

    companion object {
        private const val toolWindowId = "Dio Request"

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


    }
}