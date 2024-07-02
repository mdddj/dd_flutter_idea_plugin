package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.StateMachineEnum.*
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.bus.SocketConnectStatusMessageBus
import shop.itbug.fluttercheckversionx.socket.service.AppService
import javax.swing.JComponent


/**
 * 打开dio监听服务的状态弹窗
 */
fun Project.openSocketStatusDialog() {
    DioListenServerStateDialog(this).show()
}

class DioListenServerStateDialog(val project: Project) : DialogWrapper(project) {


    private var service: AppService = AppService.getInstance()
    private var status: StateMachineEnum? = service.dioServerStatus

    private var threadIsAlive = false


    init {
        super.init()
        title = "Dio Listen to server status"
        changeListen()
        checkDioThread()
    }


    private fun checkDioThread() {
        threadIsAlive = service.dioThread.isAlive
        repaint()
    }

    /**
     * 监听aio状态变化
     */
    private fun changeListen() {
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(SocketConnectStatusMessageBus.CHANGE_ACTION_TOPIC, object : SocketConnectStatusMessageBus {
                override fun statusChange(aioSession: AioSession?, stateMachineEnum: StateMachineEnum?) {
                    status = stateMachineEnum
                    repaint()
                }
            })
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("dio线程状态") {
                button("检测") {
                    checkDioThread()
                }
                label(if (threadIsAlive) "活跃状态" else "已关闭")
            }

        }
    }

    private val statusText: String
        get() {
            return when (status) {
                NEW_SESSION -> "连接已建立"
                INPUT_SHUTDOWN -> "读通道已被关闭"
                PROCESS_EXCEPTION -> "业务处理异常"
                DECODE_EXCEPTION -> "协议解码异常"
                INPUT_EXCEPTION -> "读操作异常"
                OUTPUT_EXCEPTION -> "写操作异常"
                SESSION_CLOSING -> "会话正在关闭中"
                SESSION_CLOSED -> "会话关闭"
                REJECT_ACCEPT -> "拒绝接受连接(Server)"
                ACCEPT_EXCEPTION -> "服务端接受连接异常"
                null -> "未知"
            }
        }

}