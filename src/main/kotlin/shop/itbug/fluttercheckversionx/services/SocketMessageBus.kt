package shop.itbug.fluttercheckversionx.services

import com.intellij.util.messages.Topic
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel


///socket 消息事件通知器
interface SocketMessageBus {

    fun handleData(data: SocketResponseModel?)

    companion object {
        val CHANGE_ACTION_TOPIC = Topic.create("dio request send", SocketMessageBus::class.java)
    }
}


///连接状态更新事件监听器
interface SocketConnectStatusMessageBus {

    /**
     * 当socket连接状态发生改变时,会发送一条事件通知出去
     */
    fun statusChange(aioSession: AioSession?,stateMachineEnum: StateMachineEnum?)

    companion object {
        val CHANGE_ACTION_TOPIC = Topic.create("socket connect status change event", SocketConnectStatusMessageBus::class.java)
    }
}
