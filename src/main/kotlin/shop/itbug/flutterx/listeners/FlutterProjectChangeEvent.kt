package shop.itbug.flutterx.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

interface FlutterProjectChangeEvent {

    fun changeProject(projectName: String, project: Project?)

    fun connectFlutterProjectChangeEvent(parentDisposable: Disposable? = null) {
        val bus = ApplicationManager.getApplication().messageBus
        val connect = if (parentDisposable != null) bus.connect(parentDisposable) else bus.connect()
        connect.subscribe(topic, this)
    }

    companion object {
        val topic = Topic.create("FlutterProjectChangeEvent", FlutterProjectChangeEvent::class.java)
    }
}