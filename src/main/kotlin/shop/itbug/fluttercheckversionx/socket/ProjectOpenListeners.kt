package shop.itbug.fluttercheckversionx.socket

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import shop.itbug.fluttercheckversionx.socket.service.AppService

class ProjectOpenListeners: StartupActivity {


    override fun runActivity(project: Project) {
        service<AppService>().initSocketService(project)
    }


}