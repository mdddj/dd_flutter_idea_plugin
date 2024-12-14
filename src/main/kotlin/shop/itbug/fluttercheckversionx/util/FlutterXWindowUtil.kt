package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

object FlutterXWindowUtil {

    /**
     * 获取window
     */
    fun getToolWindow(project: Project): ToolWindow {
        return ToolWindowManager.getInstance(project).getToolWindow("Dio Request")!!
    }

    /**
     * 显示一个基础通知
     */
    fun toast(msg: String, project: Project) {
        project.toast(msg)
    }
}