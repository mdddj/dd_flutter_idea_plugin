package shop.itbug.flutterx.bus

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import shop.itbug.flutterx.socket.Request

interface FlutterApiClickBus {
    fun click(request: Request)

    companion object {
        private val TOPIC = Topic.create("flutter api click bus", FlutterApiClickBus::class.java)
        private val bus = ApplicationManager.getApplication().messageBus
        fun fire(request: Request) {
            bus.syncPublisher(TOPIC).click(request)
        }

        fun listening(click: (request: Request) -> Unit) {
            bus.connect().subscribe(TOPIC, object : FlutterApiClickBus {
                override fun click(request: Request) {
                    click(request)
                }
            })
        }

    }
}