package shop.itbug.fluttercheckversionx.socket

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import shop.itbug.fluttercheckversionx.socket.service.AppService

class ProjectOpenListeners: ProjectManagerListener {

    override fun projectOpened(project: Project) {
        super.projectOpened(project)
//        val service = project.getService(ProjectSocketService::class.java)
//        service.onOpen(project)
        service<AppService>().initSocketService();
    }
}