package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.widget.MyComboActionNew


/**
 * 过滤项目
 * 因为可能会多开多个项目,所以要支持过滤
 * 当然socket也根据项目分离Request请求
 */
class ProjectFilter : MyComboActionNew.ComboBoxSettingAction<String>() {


    override val reGetActions: Boolean
        get() = true


    override val availableOptions: MutableList<String>
        get() = AppService.getInstance().flutterProjects.keys.toList().toMutableList()


    ///
    override var value: String
        get() = AppService.getInstance().currentSelectName.get() ?: ""
        set(v) {
            println(v)
        }


    override fun update(e: AnActionEvent) {
        super.update(e)
        if (AppService.getInstance().flutterProjects.keys.isEmpty()) {
            e.presentation.isEnabled = false
            e.presentation.text = PluginBundle.get("empty")
        }
        e.presentation.icon = MyIcons.flutter
    }

    override fun getText(option: String): String {
        return option
    }


    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun setNewValue(value: String, e: AnActionEvent) {
        service<AppService>().changeCurrentSelectFlutterProjectName(value, e.project)
    }

    companion object {

        ///获取实例
        val instance: ProjectFilter = ActionManager.getInstance().getAction("FlutterProjects") as ProjectFilter
    }
}



