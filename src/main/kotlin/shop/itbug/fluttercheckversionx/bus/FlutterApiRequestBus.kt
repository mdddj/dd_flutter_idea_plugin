package shop.itbug.fluttercheckversionx.bus

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService

interface SocketMessageBus {

    fun handleData(data: ProjectSocketService.SocketResponseModel?)

    companion object {
       private  val CHANGE_ACTION_TOPIC = Topic.create("dio request send", SocketMessageBus::class.java)
        private val messageBus = ApplicationManager.getApplication().messageBus

        /**
         * 将模型发送给监听者
         */
        fun fire(model: Request) {
            messageBus.syncPublisher(CHANGE_ACTION_TOPIC).handleData(model)
        }

        /**'
         * 监听api进入
         */
        fun listening(apiCallback:(req: Request)->Unit) {
            messageBus.connect().subscribe(CHANGE_ACTION_TOPIC,object : SocketMessageBus{
                override fun handleData(data: ProjectSocketService.SocketResponseModel?) {
                    data?.let { apiCallback.invoke(it) }
                }
            })
        }


    }
}
