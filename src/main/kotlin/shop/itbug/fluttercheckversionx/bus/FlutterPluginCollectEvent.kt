package shop.itbug.fluttercheckversionx.bus

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic


enum class FlutterPluginCollectEventType {
    add,remove
}

interface FlutterPluginCollectEvent {

    fun handle(type: FlutterPluginCollectEventType,pluginName: String)

    companion object {
        private val bus = ApplicationManager.getApplication().messageBus
        private val TOPIC = Topic.create("FlutterPluginCollectEvent",FlutterPluginCollectEvent::class.java)

        /**
         * 通知
         */
        fun fire(type: FlutterPluginCollectEventType,pluginName: String) {
            bus.syncPublisher(TOPIC).handle(type, pluginName)
        }


        fun listen(call: (type: FlutterPluginCollectEventType, pluginName: String)->Unit) {
            bus.connect().subscribe(TOPIC,object : FlutterPluginCollectEvent {
                override fun handle(type: FlutterPluginCollectEventType, pluginName: String) {
                    call.invoke(type, pluginName)
                }
            })
        }
    }
}