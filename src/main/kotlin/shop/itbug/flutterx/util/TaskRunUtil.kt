package shop.itbug.flutterx.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

//执行任务工具
object TaskRunUtil {

    /**
     * 在后台线程中执行任务
     *
     * @param project 项目对象，用于关联后台任务
     * @param title 任务标题，默认为"FlutterX"
     * @param task 要执行的任务函数，接收进度指示器作为参数
     */
    fun runBackground(project: Project, title: String = "FlutterX", task: (indicator: ProgressIndicator) -> Unit) {
        // 创建后台任务执行器
        val taskExecutor = object : Task.Backgroundable(project, title) {
            override fun run(p0: ProgressIndicator) {
                task.invoke(p0)
            }
        }
        // 执行后台任务
        ProgressManager.getInstance().run(taskExecutor)
    }

    /**
     * 运行一个模态任务对话框
     *
     * @param project 项目对象，用于关联IDE的项目上下文
     * @param title 任务对话框的标题，默认为"FlutterX"
     * @param task 需要执行的任务函数，接收一个进度指示器参数
     */
    fun runModal(project: Project, title: String = "FlutterX", task: (indicator: ProgressIndicator) -> Unit) {
        // 创建模态任务执行器，继承自IDE的模态任务类
        val taskExecutor = object : Task.Modal(project, title, true) {
            override fun run(p0: ProgressIndicator) {
                task.invoke(p0)
            }
        }
        // 通过进度管理器运行任务
        ProgressManager.getInstance().run(taskExecutor)
    }

    

}