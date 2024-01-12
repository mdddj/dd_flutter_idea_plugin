package shop.itbug.fluttercheckversionx.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.window.logger.MyLogInfo

interface MyLoggerEvent {

    fun addLog(log: MyLogInfo)

    companion object {
        private val topic = Topic.create("logger-event-my", MyLoggerEvent::class.java)
        private val bus = ApplicationManager.getApplication().messageBus

        ///执行
        fun fire(log: MyLogInfo) {
            bus.syncPublisher(topic).addLog(log)
        }

        ///监听
        fun listen(onLog: (logger: MyLogInfo) -> Unit) {
            bus.connect().subscribe(topic, object : MyLoggerEvent {
                override fun addLog(log: MyLogInfo) {
                    onLog(log)
                }
            })
        }

    }
}