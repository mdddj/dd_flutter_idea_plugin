package shop.itbug.fluttercheckversionx.setting

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.util.getPubspecYAMLFile
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities

/**
 * 管理忽略检测更新的包
 */
class IgPluginPubspecConfigList(val project: Project) : BorderLayoutPanel() {

    private val manager = DartPluginIgnoreConfig.getInstance(project)
    private val list: JBList<String> = JBList<String>().apply {
    }
    private val decorator: ToolbarDecorator = ToolbarDecorator.createDecorator(list).apply {
        this.setRemoveAction {
            manager.remove(list.selectedValue)
            refresh()
            project.getPubspecYAMLFile()?.let {
                DaemonCodeAnalyzer.getInstance(project).restart(it)
            }

        }
        this.disableUpDownActions()
    }


    private fun refresh() {
        val names = DartPluginIgnoreConfig.getInstance(project).names
        list.model = DefaultListModel<String?>().apply { addAll(names) }
    }

    init {
        addToCenter(decorator.createPanel())
        SwingUtilities.invokeLater {
            refresh()
        }
    }

    companion object {

        ///显示为一个 popup
        fun showInPopup(project: Project) {
            val popup =
                JBPopupFactory.getInstance().createComponentPopupBuilder(IgPluginPubspecConfigList(project), null)
                    .createPopup()
            popup.showCenteredInCurrentWindow(project)
        }
    }

}