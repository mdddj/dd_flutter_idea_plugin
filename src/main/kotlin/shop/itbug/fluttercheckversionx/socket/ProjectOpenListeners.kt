package shop.itbug.fluttercheckversionx.socket

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.WindowManager
import shop.itbug.fluttercheckversionx.socket.service.AppService

class ProjectOpenListeners: ProjectManagerListener {

    override fun projectOpened(project: Project) {
        super.projectOpened(project)
        val appService = service<AppService>()
        appService.initSocketService(project)
        appService.initExampleLabels()
    }
}