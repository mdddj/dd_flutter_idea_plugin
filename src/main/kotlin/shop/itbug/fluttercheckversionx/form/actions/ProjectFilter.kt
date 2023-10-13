package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.ModalityUiUtil
import shop.itbug.fluttercheckversionx.bus.ProjectListChangeBus
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.projectClosed
import shop.itbug.fluttercheckversionx.widget.MyComboActionNew
import java.util.*


/**
 * 过滤项目
 * 因为可能会多开多个项目,所以要支持过滤
 * 当然socket也根据项目分离Request请求
 */
class ProjectFilter : MyComboActionNew.ComboBoxSettingAction<String>() {


    private var projectNames = emptyList<String>()
    private var ideaProject: MutableList<Project> = Collections.synchronizedList(mutableListOf())
    private val appService = service<AppService>()


    override fun update(e: AnActionEvent) {

        e.project?.apply {
            if (!ideaProject.contains(this)) {
                ideaProject.add(this)
                val changeNameRunnable = Runnable {}
                appService.addListening(changeNameRunnable)
                this.projectClosed {
                    appService.removeListening(changeNameRunnable)
                }
                ProjectListChangeBus.lisening {
                    projectNames = it
                }

            }
        }
        doUpdate(e)
        super.update(e)

    }

    override val reGetActions: Boolean
        get() = true


    override val availableOptions: MutableList<String>
        get() = projectNames.toMutableList()


    override var value: String
        get() = appService.currentSelectName.get() ?: ""
        set(value) {
            service<AppService>().changeCurrentSelectFlutterProjectName(value)
            ActivityTracker.getInstance().inc()
        }

    override fun getText(option: String): String {
        return option
    }


    private fun doUpdate(e: AnActionEvent) {
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState()) {
            updateSelect(e)
        }
    }


    //更新选中
    private fun updateSelect(e: AnActionEvent) {
        if (projectNames.isEmpty()) {
            e.presentation.isEnabled = false
            changeText(e.presentation, PluginBundle.get("empty"))
        } else {
            e.presentation.isEnabled = true
        }

        val appName = appService.currentSelectName.get()
        if (appName != null) {
            changeText(e.presentation, appName)
        }
        if (appName == null && projectNames.size == 1) {
            appService.changeCurrentSelectFlutterProjectName(projectNames[0])
        }

        //主要是解决新开项目后选项会被禁用的问题
        if (appName != null && projectNames.isEmpty()) {
            projectNames = appService.projectNames
        }
    }

    private fun changeText(presentation: Presentation, name: String) {
        with(presentation) {
            text = name
            icon = MyIcons.flutter
            description = name
        }
    }


    override fun getActionUpdateThread() = ActionUpdateThread.BGT


}



