package shop.itbug.flutterx.form.components

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.i18n.PluginBundle


///显示设置
enum class DioUiSettingMenu(val title: String) {
    Domain(PluginBundle.get("display_domain_name")),
    Params(PluginBundle.get("display.query.parameters")),
    Method(PluginBundle.get("show.request.method")),
    Status(PluginBundle.get("display.status.code")),
    Time(PluginBundle.get("display.time")),
    RequestTime(PluginBundle.get("time")),
    ProjectName(PluginBundle.get("dio.setting.project.name.show.option")),
    DataSize(PluginBundle.get("dio.setting.show.data.size"))

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
        val setting = DioListingUiConfig.setting
        return when (menu) {
            DioUiSettingMenu.Domain -> setting.showHost
            DioUiSettingMenu.Params -> setting.showQueryParams
            DioUiSettingMenu.Method -> setting.showMethod
            DioUiSettingMenu.Status -> setting.showStatusCode
            DioUiSettingMenu.Time -> setting.showTimestamp
            DioUiSettingMenu.RequestTime -> setting.showDate
            DioUiSettingMenu.ProjectName -> setting.showProjectName
            DioUiSettingMenu.DataSize -> setting.showDataSize
        }
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        when (menu) {
            DioUiSettingMenu.Domain -> DioListingUiConfig.changeSetting { it.copy(showHost = state) }
            DioUiSettingMenu.Params -> DioListingUiConfig.changeSetting { it.copy(showQueryParams = state) }
            DioUiSettingMenu.Method -> DioListingUiConfig.changeSetting { it.copy(showMethod = state) }
            DioUiSettingMenu.Status -> DioListingUiConfig.changeSetting { it.copy(showStatusCode = state) }
            DioUiSettingMenu.Time -> DioListingUiConfig.changeSetting { it.copy(showTimestamp = state) }
            DioUiSettingMenu.RequestTime -> DioListingUiConfig.changeSetting { it.copy(showDate = state) }
            DioUiSettingMenu.ProjectName -> DioListingUiConfig.changeSetting { it.copy(showProjectName = state) }
            DioUiSettingMenu.DataSize -> DioListingUiConfig.changeSetting { it.copy(showDataSize = state) }
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