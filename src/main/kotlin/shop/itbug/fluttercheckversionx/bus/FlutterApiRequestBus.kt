package shop.itbug.fluttercheckversionx.bus

import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService

interface SocketMessageBus {

    fun handleData(data: ProjectSocketService.SocketResponseModel?)

    companion object {
        val CHANGE_ACTION_TOPIC = Topic.create("dio request send", SocketMessageBus::class.java)
    }
}
