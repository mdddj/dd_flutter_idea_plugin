package shop.itbug.fluttercheckversionx.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

interface FlutterProjectChangeEvent {
    fun changeProject(projectName: String)

    fun connectFlutterProjectChangeEvent() {
        ApplicationManager.getApplication().messageBus.connect().subscribe(topic, this)
    }

    companion object {
        val topic = Topic.create("FlutterProjectChangeEvent", FlutterProjectChangeEvent::class.java)
    }
}