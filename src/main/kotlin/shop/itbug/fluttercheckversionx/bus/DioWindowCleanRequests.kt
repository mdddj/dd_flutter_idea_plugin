package shop.itbug.fluttercheckversionx.bus

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

interface DioWindowCleanRequests {

    /**
     * 情况列表
     */
    fun clean()

    companion object {
        private val TOPIC = Topic.create("DioWindowCleanRequests",DioWindowCleanRequests::class.java)
        private val bus = ApplicationManager.getApplication().messageBus

        fun fire() {
            bus.syncPublisher(TOPIC).clean()
        }

        fun listening(clean: ()->Unit){
            bus.connect().subscribe(TOPIC,object : DioWindowCleanRequests {
                override fun clean() {
                    clean.invoke()
                }
            })
        }
    }
}