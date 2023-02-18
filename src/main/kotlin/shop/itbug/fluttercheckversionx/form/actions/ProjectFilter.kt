package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import shop.itbug.fluttercheckversionx.bus.ProjectListChangeBus
import shop.itbug.fluttercheckversionx.form.MyProjectProviders
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.projectClosed
import java.util.*
import javax.swing.JComponent


/**
 * 过滤项目
 * 因为可能会多开多个项目,所以要支持过滤
 * 当然socket也根据项目分离Request请求
 */
class ProjectFilter : ComboBoxAction(), DumbAware {


    private val actions = mutableListOf<ProjectAnAction>()
    private var projects: List<String> by MyProjectProviders()
    private var ideaProject: MutableList<Project> = Collections.synchronizedList(mutableListOf())

    private fun createDefaultGroup(): DefaultActionGroup {
        val group = DefaultActionGroup()
        group.addAll(actions)
        return group
    }

    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        println("...createPopupActionGroup...")
        return createDefaultGroup()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = projects.isNotEmpty()
        e.presentation.text = "暂无项目"
        e.presentation.icon = MyIcons.flutter
        if (e.project != null && !ideaProject.contains(e.project)) {
            ideaProject.add(e.project!!)
            val changeNameRunnable = Runnable { updateSelect(e) }
            service<AppService>().addListening(changeNameRunnable)
            e.project?.projectClosed { service<AppService>().removeListening(changeNameRunnable) }
            ProjectListChangeBus.lisening {
                updateSelect(e)
            }
        }
        updateSelect(e)
    }

    //更新选中
    private fun updateSelect(e: AnActionEvent) {
        actions.clear()
        actions.addAll(projects.map { ProjectAnAction(it) })
        val appName = service<AppService>().currentSelectName.get()

        if (appName != null) {
            changeText(e.presentation, appName)
        }

        if (appName == null && projects.size == 1) {
            //给它自动选中
            changeText(e.presentation, projects[0])
        }

        ActivityTracker.getInstance().inc()
    }

    private fun changeText(presentation: Presentation, name: String) {
        presentation.text = name
        presentation.icon = MyIcons.flutter
    }

    override fun getPreselectCondition(): Condition<AnAction> {
        return Condition<AnAction> { t -> t is ProjectAnAction && t.getText() == service<AppService>().currentSelectName.get() }
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * 项目选择操作
 */
class ProjectAnAction(private val projectName: String) :
    DumbAwareAction({ projectName }, MyIcons.flutter) {
    override fun actionPerformed(e: AnActionEvent) {
        service<AppService>().changeCurrentSelectFlutterProjectName(projectName)
    }

    fun getText() = projectName
}