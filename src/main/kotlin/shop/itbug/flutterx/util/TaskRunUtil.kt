package shop.itbug.flutterx.util

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
internal class BackgroundProgressScope(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return cs.launch {
            if (!project.isDisposed) {
                block()
            }
        }
    }
}

fun Project.launchBackgroundProgress(
    title: String,
    cancellable: Boolean = true,
    action: suspend CoroutineScope.() -> Unit,
): Job {
    return service<BackgroundProgressScope>().launch {
        if (cancellable) {
            withBackgroundProgress(this@launchBackgroundProgress, title, action)
        } else {
            withBackgroundProgress(this@launchBackgroundProgress, title, false, action)
        }
    }
}

//执行任务工具
object TaskRunUtil {

    /**
     * 在后台线程中执行任务
     *
     * @param project 项目对象，用于关联后台任务
     * @param title 任务标题，默认为"FlutterX"
     * @param task 要执行的任务
     */
    fun runBackground(project: Project, title: String = "FlutterX", task: suspend CoroutineScope.() -> Unit) {
        project.launchBackgroundProgress(title, action = task)
    }

    /**
     * 在 EDT 上打开可取消的模态进度框。
     *
     * @param project 项目对象，用于关联IDE的项目上下文
     * @param title 任务对话框的标题，默认为"FlutterX"
     * @param task 需要执行的任务
     */
    fun runModal(project: Project, title: String = "FlutterX", task: suspend CoroutineScope.() -> Unit) {
        runWithModalProgressBlocking(project, title) {
            task()
        }
    }


}
