package shop.itbug.flutterx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import shop.itbug.flutterx.config.FlutterXGlobalConfigService
import shop.itbug.flutterx.i18n.PluginBundle
import java.awt.Dimension
import javax.swing.DefaultListModel


/**
 * 忽略flutter版本本次更新
 */
class FlutterVersionIgnoreAction(val version: String, val actionClick: () -> Unit) : DumbAwareAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val pluginConfig = FlutterXGlobalConfigService.getInstance()
        pluginConfig.state.ignoreFlutterVersions.add(version)
        pluginConfig.updateData()
        actionClick()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = PluginBundle.get("ignore_this_version_to_check")
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

private val getVersions get() = FlutterXGlobalConfigService.getInstance().state.ignoreFlutterVersions


//被忽略的版本列表
class FlutterVersionIgnoreList : JBList<String>(DefaultListModel()) {
    private val myModel get() = model as DefaultListModel<String>
    private val actionGroup = DefaultActionGroup()
    private val removeActin = object : DumbAwareAction(PluginBundle.get("delete_base_text")) {
        override fun actionPerformed(p0: AnActionEvent) {
            FlutterXGlobalConfigService.getInstance().updateBase {
                it.ignoreFlutterVersions.remove(selectedValue)
            }
            myModel.clear()
            myModel.addAll(getVersions)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = selectedValue != null
            super.update(e)
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }
    }

    init {
        this.preferredSize = Dimension(240, 60)
        border = JBUI.Borders.customLine(JBColor.border())
        myModel.addAll(getVersions)
        actionGroup.add(removeActin)
        PopupHandler.installPopupMenu(this, actionGroup,"FlutterVersionIgnoreList")
        ListUiUtil.Selection.installSelectionOnRightClick(this)
    }
}