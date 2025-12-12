package shop.itbug.flutterx.bus

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

interface DioWindowCleanRequests {

    /**
     * 情况列表
     */
    fun clean()

    companion object {
        private val TOPIC = Topic.create("DioWindowCleanRequests", DioWindowCleanRequests::class.java)
        private val bus = ApplicationManager.getApplication().messageBus

        fun fire() {
            bus.syncPublisher(TOPIC).clean()
        }

        fun listening(parentDisposable: Disposable, clean: () -> Unit) {
            bus.connect(parentDisposable).subscribe(TOPIC, object : DioWindowCleanRequests {
                override fun clean() {
                    clean.invoke()
                }
            })
        }
    }
}