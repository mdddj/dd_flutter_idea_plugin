package socket

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class ProjectOpenListeners: ProjectManagerListener {

    override fun projectOpened(project: Project) {
        super.projectOpened(project)
        val service = project.getService(ProjectSocketService::class.java)
        service.onOpen(project)
    }
}