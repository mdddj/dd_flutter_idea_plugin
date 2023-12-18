package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener


/**
 * Project关闭监听事件
 */
fun Project.projectClosed(call: () -> Unit): ProjectManagerListener {
    return ProjectUtil.closedHandle(this, call)
}

object ProjectUtil {
    /**
     * 添加project关闭监听
     */
    fun closedHandle(project: Project, call: () -> Unit): ProjectManagerListener {
        val listen = object : ProjectManagerListener {
            override fun projectClosed(project: Project) {
                call.invoke()
                super.projectClosed(project)
            }
        }
        ProjectManager.getInstance().addProjectManagerListener(project, listen)
        return listen
    }
}
