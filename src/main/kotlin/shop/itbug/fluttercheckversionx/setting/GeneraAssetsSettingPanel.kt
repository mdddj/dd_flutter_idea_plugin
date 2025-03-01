package shop.itbug.fluttercheckversionx.setting

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.constance.Links
import shop.itbug.fluttercheckversionx.dialog.MyRowBuild
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities


typealias GeneraAssetsSettingPanelIsModified = (value: Boolean) -> Unit

/**
 * 生成资产文件的设置面板
 */
class GeneraAssetsSettingPanel(
    project: Project,
    var settingModel: GenerateAssetsClassConfigModel,
    val parentDisposable: Disposable,
    modified: GeneraAssetsSettingPanelIsModified,

    ) :
    BorderLayoutPanel() {

    //忽略的文件
    private val igFilesWidget = IgFileList(project)


    private val dialogPanel = getGeneraAssetsPanel(project, settingModel, parentDisposable, modified)


    init {
        addToTop(createToolBar())
        addToCenter(igFilesWidget)
        addToRight(createRightSettingPanel())
    }

    private fun createRightSettingPanel() = BorderLayoutPanel().apply {
        addToCenter(dialogPanel)
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
    }

    private fun createToolBar() = BorderLayoutPanel().apply {
        val actionGroup = DefaultActionGroup(*createActions())
        val toolbar =
            ActionManager.getInstance().createActionToolbar("GenerateAssetsIgFileToolbar", actionGroup, true).apply {
                targetComponent = igFilesWidget
            }

        addToLeft(toolbar.component)
    }

    private fun createActions(): Array<AnAction> = arrayOf(
        object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                WidgetUtil.getTextEditorPopup(PluginBundle.get("g.13"), "", null, {
                    it.show(RelativePoint.fromScreen(igFilesWidget.locationOnScreen))
                }) {
                    this@GeneraAssetsSettingPanel.igFilesWidget.addItemString(it)
                }
            }
        },
        object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                val selectedValue = igFilesWidget.selectedValue
                if (selectedValue != null) {
                    this@GeneraAssetsSettingPanel.igFilesWidget.removeItem(selectedValue)
                }
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = igFilesWidget.selectedValue != null
                super.update(e)
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }

        },
        WidgetUtil.getHelpAnAction { it ->
            it.inputEvent?.let {
                WidgetUtil.showTopBalloon(it.component, PluginBundle.get("g.14"))
            }

        },
    )


    fun doApply() {
        dialogPanel.apply()
    }
}


/**
 * 设置面板
 */
fun getGeneraAssetsPanel(
    project: Project,
    settingModel: GenerateAssetsClassConfigModel,
    parentDisposable: Disposable,
    isModified: GeneraAssetsSettingPanelIsModified
): DialogPanel {


    val p: DialogPanel = panel {

        row {
            comment(Links.generateDocCommit(Links.assets))
        }

        row(PluginBundle.get("g.1")) {
            textField().bindText({ settingModel.className ?: "" }, { settingModel.className = it })
        }


        row(PluginBundle.get("g.2")) {
            textField().bindText({ settingModel.fileName ?: "" }, {
                settingModel.fileName = it
            })
        }
        row(PluginBundle.get("g.3")) {
            MyRowBuild.folder(this, { settingModel.path ?: "" }, { settingModel.path = it }, project)
        }.contextHelp(PluginBundle.get("g.3.t"), PluginBundle.get("g.3.t1"))
        row(PluginBundle.get("g.4")) {
            textField().bindText({ settingModel.replaceTags ?: "" }, { settingModel.replaceTags = it })
        }.contextHelp(PluginBundle.get("g.4.1"), PluginBundle.get("g.4"))
        row(PluginBundle.get("g.5.1")) {
            checkBox(PluginBundle.get("g.5.2")).bindSelected(settingModel::firstChatUpper)
        }
        row(PluginBundle.get("g.6.1")) {
            checkBox(PluginBundle.get("g.6.2")).bindSelected(settingModel::addFolderNamePrefix)
        }
        row(PluginBundle.get("g.7.1")) {
            checkBox(PluginBundle.get("g.7.2")).bindSelected(settingModel::addFileTypeSuffix)
        }
        row(PluginBundle.get("g.8.1")) {
            checkBox(PluginBundle.get("g.8.2")).bindSelected(settingModel::dontTip)
        }
        row(PluginBundle.get("g.9.1")) {
            checkBox(PluginBundle.get("g.9.2")).bindSelected(settingModel::autoListenFileChange)
        }
//        row(PluginBundle.get("g.10.1")) {
//            checkBox(PluginBundle.get("g.10.2")).bindSelected(settingModel::showImageIconInEditor)
//        }
    }

    val alarm = Alarm(parentDisposable)


    fun initValidation() {
        alarm.addRequest({
            isModified.invoke(p.isModified())
            initValidation()
        }, 1000)
    }

    SwingUtilities.invokeLater {
        initValidation()
    }

    p.registerValidators(parentDisposable)

    return p
}


/**
 * 忽略的文件或者后缀
 */
class IgFileList(val project: Project) : JBList<String>() {

    init {
        cellRenderer = DefaultListCellRenderer()
        model = DefaultListModel<String?>().apply {
            addAll(GenerateAssetsClassConfig.getGenerateAssetsSetting(project).igFiles)
        }
        border = BorderFactory.createLineBorder(JBColor.border())
        emptyText.appendText(PluginBundle.get("g.12"))
    }

    fun addItemString(name: String) {
        (model as DefaultListModel).addElement(name)
        GenerateAssetsClassConfig.getGenerateAssetsSetting(project).addIgFiles(name)
    }

    fun removeItem(name: String) {
        (model as DefaultListModel).removeElement(name)
        GenerateAssetsClassConfig.getGenerateAssetsSetting(project).removeIgFiles(name)
    }

}

