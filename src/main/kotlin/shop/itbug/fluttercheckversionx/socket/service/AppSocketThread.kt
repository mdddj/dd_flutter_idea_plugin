package shop.itbug.fluttercheckversionx.socket.service

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.smartboot.socket.transport.AioQuickServer
import shop.itbug.fluttercheckversionx.util.MyNotifactionUtil
typealias SocketStateResult = (state: Boolean) -> Unit
class AppSocketThread(private val server: AioQuickServer,private val project: Project,val state: SocketStateResult): Runnable {
    override fun run() {
        try {
            server.start()
            state(true)
        } catch (e: Exception) {
            state(false)
        }
    }

}