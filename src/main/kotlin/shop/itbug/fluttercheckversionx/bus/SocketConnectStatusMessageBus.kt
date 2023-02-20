package shop.itbug.fluttercheckversionx.bus

import com.intellij.util.messages.Topic
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioSession

///连接状态更新事件监听器
interface SocketConnectStatusMessageBus {

    /**
     * 当socket连接状态发生改变时,会发送一条事件通知出去
     */
    fun statusChange(aioSession: AioSession?, stateMachineEnum: StateMachineEnum?)

    companion object {
        val CHANGE_ACTION_TOPIC = Topic.create("socket connect status change event", SocketConnectStatusMessageBus::class.java)
    }
}
