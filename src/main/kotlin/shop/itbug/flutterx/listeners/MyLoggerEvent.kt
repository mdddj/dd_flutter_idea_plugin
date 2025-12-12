package shop.itbug.flutterx.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import shop.itbug.flutterx.window.logger.MyLogInfo

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
        fun listen(parentDisposer: Disposable, onLog: (logger: MyLogInfo) -> Unit) {
            bus.connect(parentDisposer).subscribe(topic, object : MyLoggerEvent {
                override fun addLog(log: MyLogInfo) {
                    onLog(log)
                }
            })
        }

    }
}