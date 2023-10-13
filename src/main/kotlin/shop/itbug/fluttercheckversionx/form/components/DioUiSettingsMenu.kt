package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import shop.itbug.fluttercheckversionx.config.DioxListingUiConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle


///显示设置
enum class DioUiSettingMenu(val title: String) {
    Domain("${PluginBundle.get("display_domain_name")} (Host)"),
    Params("${PluginBundle.get("display.query.parameters")} (Url params)"),
    Method("${PluginBundle.get("show.request.method")} (Method)"),
    Status("${PluginBundle.get("display.status.code")} (Status code)"),
    Time("${PluginBundle.get("display.time")} (Time consuming)"),
    RequestTime("${PluginBundle.get("time")} (Request time)"),
    ProjectName("${PluginBundle.get("dio.setting.project.name.show.option")} (Project Name)")
}


internal class DioUIShowActionGroup : DumbAware, ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val actions = ArrayList<AnAction>()
        actions.addAll(DioUiSettingMenu.entries.map { DioUiRenderOption(it) })
        return actions.toTypedArray()
    }
}


private class DioUiRenderOption(val menu: DioUiSettingMenu) : DumbAwareToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val setting = DioxListingUiConfig.setting
        return when (menu) {
            DioUiSettingMenu.Domain -> setting.showHost
            DioUiSettingMenu.Params -> setting.showQueryParams
            DioUiSettingMenu.Method -> setting.showMethod
            DioUiSettingMenu.Status -> setting.showStatusCode
            DioUiSettingMenu.Time -> setting.showTimestamp
            DioUiSettingMenu.RequestTime -> setting.showDate
            DioUiSettingMenu.ProjectName -> setting.showProjectName
        }
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        when (menu) {
            DioUiSettingMenu.Domain -> DioxListingUiConfig.changeSetting { it.copy(showHost = state) }
            DioUiSettingMenu.Params -> DioxListingUiConfig.changeSetting { it.copy(showQueryParams = state) }
            DioUiSettingMenu.Method -> DioxListingUiConfig.changeSetting { it.copy(showMethod = state) }
            DioUiSettingMenu.Status -> DioxListingUiConfig.changeSetting { it.copy(showStatusCode = state) }
            DioUiSettingMenu.Time -> DioxListingUiConfig.changeSetting { it.copy(showTimestamp = state) }
            DioUiSettingMenu.RequestTime -> DioxListingUiConfig.changeSetting { it.copy(showDate = state) }
            DioUiSettingMenu.ProjectName -> DioxListingUiConfig.changeSetting { it.copy(showProjectName = state) }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = menu.title
        Toggleable.setSelected(e.presentation, isSelected(e))
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}