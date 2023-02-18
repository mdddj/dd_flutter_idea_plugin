package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.util.projectClosed
import java.util.*
import javax.swing.JComponent


/**
 * 筛选状态码
 */
class MethodFilter : ComboBoxAction() {

    private val all = "ALL"
    private val methodActions = mutableListOf<MethodAnAction>()
    private val appService = service<AppService>()
    private val projects: MutableList<Project> = Collections.synchronizedList(mutableListOf())

    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        methodActions.clear()
        val currentSelect = appService.currentSelectName.get()
        if (currentSelect != null) {
            val apis = appService.getRequestsWithProjectName(currentSelect)
            val methods = apis.map { it.method ?: "" }.toSet()
            methodActions.addAll(methods.map { MethodAnAction(it) })
            methodActions.add(0, MethodAnAction(all))
            return DefaultActionGroup(methodActions)
        }
        return DefaultActionGroup()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (methodActions.isEmpty()) {
            changeText(e, PluginBundle.get("empty"))
            e.presentation.isEnabled = methodActions.isNotEmpty()
        }
        e.project?.let {
            if (!projects.contains(it)) {
                projects.add(it)
                val runnable = Runnable { doUpdate() }
                appService.addListening(runnable)
                it.projectClosed {
                    appService.removeListening(runnable)
                }
            }
        }
        doUpdate()
    }


    /**
     * 执行更新
     */
    private fun doUpdate() {
        val currType = appService.currentSelectMethodType.get()
        if (currType == null && methodActions.size == 1) {
            setDefault()
        }
    }

    /**
     * 选中
     */
    private fun setDefault() {
        appService.changeCurrentSelectFilterMethodType(all)
    }

    /**
     * 更新显示文本
     */
    private fun changeText(e: AnActionEvent, text: String) {
        e.presentation.text = text
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}

class MethodAnAction(val method: String) : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        service<AppService>().changeCurrentSelectFilterMethodType(method)
    }
}