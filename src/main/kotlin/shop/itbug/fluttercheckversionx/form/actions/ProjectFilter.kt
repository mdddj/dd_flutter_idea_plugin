package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.util.ModalityUiUtil
import shop.itbug.fluttercheckversionx.bus.ProjectListChangeBus
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
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
    private var projectNames = emptyList<String>()
    private var ideaProject: MutableList<Project> = Collections.synchronizedList(mutableListOf())
    private val appService = service<AppService>()

    private fun createDefaultGroup(): DefaultActionGroup {
        val group = DefaultActionGroup()
        group.addAll(actions)
        return group
    }

    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        return createDefaultGroup()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.project?.apply {
            if (!ideaProject.contains(this)) {
                ideaProject.add(this)
                val changeNameRunnable = Runnable {
                    doUpdate(e)
                }
                appService.addListening(changeNameRunnable)
                this.projectClosed {
                    appService.removeListening(changeNameRunnable)
                }
                ProjectListChangeBus.lisening {
                    projectNames = it
                    changeProjectNameAction()
                    doUpdate(e)
                }

            }
        }
        doUpdate(e)
    }


    private fun changeProjectNameAction() {
        actions.clear()
        actions.addAll(projectNames.map { ProjectAnAction(it) })
    }

     private fun doUpdate(e: AnActionEvent) {
         ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState()) {
             updateSelect(e)
         }
     }


    //更新选中
    private fun updateSelect(e: AnActionEvent) {
        if(actions.isEmpty()){
            e.presentation.isEnabled = false
            e.presentation.text = PluginBundle.get("empty")
            e.presentation.icon = MyIcons.flutter
        }
        val appName = appService.currentSelectName.get()
        if (appName != null) {
            changeText(e.presentation, appName)
        }
        if (appName == null && actions.size == 1) {
            appService.changeCurrentSelectFlutterProjectName(actions[0].getText())
        }
    }

    private fun changeText(presentation: Presentation, name: String) {
        presentation.text = name
        presentation.icon = MyIcons.flutter
    }

    override fun getPreselectCondition(): Condition<AnAction> {
        return Condition<AnAction> { t -> t is ProjectAnAction && t.getText() == appService.currentSelectName.get() }
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * 项目选择操作
 */
class ProjectAnAction(private val projectName: String) : AnAction({ projectName }, MyIcons.flutter) {
    override fun actionPerformed(e: AnActionEvent) {
        service<AppService>().changeCurrentSelectFlutterProjectName(projectName)
    }

    fun getText() = projectName
}