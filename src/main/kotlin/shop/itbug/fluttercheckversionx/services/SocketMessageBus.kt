package shop.itbug.fluttercheckversionx.services

import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel

interface SocketMessageBus {

    fun handleData(data: SocketResponseModel?)

    companion object {
        val CHANGE_ACTION_TOPIC = Topic.create("dio request send", SocketMessageBus::class.java)
    }
}


class SocketMessageBusHandle(val  handle:(data: SocketResponseModel)->Unit): SocketMessageBus {
    override fun handleData(data: SocketResponseModel?) {
        if(data!=null){
            handle(data)
        }
    }

}