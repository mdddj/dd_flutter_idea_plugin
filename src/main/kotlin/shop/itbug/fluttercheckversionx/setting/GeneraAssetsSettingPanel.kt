package shop.itbug.fluttercheckversionx.setting

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.vcs.commit.NonModalCommitPanel.Companion.showAbove
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
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
    var settingModel: GenerateAssetsClassConfigModel,
    val parentDisposable: Disposable,
    modified: GeneraAssetsSettingPanelIsModified
) :
    BorderLayoutPanel() {

    //忽略的文件
    private val igFilesWidget = IgFileList()


    private val dialogPanel = getGeneraAssetsPanel(settingModel, parentDisposable, modified)


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
                setReservePlaceAutoPopupIcon(true)
                layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
                targetComponent = igFilesWidget
            }

        addToLeft(toolbar.component)
    }

    private fun createActions(): Array<AnAction> = arrayOf(
        object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                WidgetUtil.getTextEditorPopup(PluginBundle.get("g.13"), "", {
                    it.showAbove(igFilesWidget)
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

        },
        WidgetUtil.getHelpAnAction { it ->
            it.inputEvent?.let {
                WidgetUtil.showTopBalloon(it.component, PluginBundle.get("g.14"))
            }

        },
        WidgetUtil.getMoneyAnAction()
    )


    fun doApply() {
        dialogPanel.apply()
    }
}


/**
 * 设置面板
 */
fun getGeneraAssetsPanel(
    settingModel: GenerateAssetsClassConfigModel,
    parentDisposable: Disposable,
    isModified: GeneraAssetsSettingPanelIsModified
): DialogPanel {


    val p: DialogPanel = panel {
        row(PluginBundle.get("g.1")) {
            textField().bindText({ settingModel.className }, {
                GenerateAssetsClassConfig.getGenerateAssetsSetting().className = it
            })
        }
        row(PluginBundle.get("g.2")) {
            textField().bindText({ settingModel.fileName }, {
                GenerateAssetsClassConfig.getGenerateAssetsSetting().fileName = it
            })
        }
        row(PluginBundle.get("g.3")) {
            textField().bindText({ settingModel.path }, {
                GenerateAssetsClassConfig.getGenerateAssetsSetting().path = it
            })
        }.contextHelp(PluginBundle.get("g.3.t"), PluginBundle.get("g.3.t1"))
        row(PluginBundle.get("g.4")) {
            textField().bindText(settingModel::replaceTags)
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
        row(PluginBundle.get("g.10.1")) {
            checkBox(PluginBundle.get("g.10.2")).bindSelected(settingModel::showImageIconInEditor)
        }
        row {
            label(PluginBundle.get("g.11")).component.apply {
                font = JBFont.small()
                foreground = JBUI.CurrentTheme.Link.Foreground.DISABLED
            }
        }
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

    val disposable = Disposer.newDisposable()
    p.registerValidators(disposable)
    Disposer.register(parentDisposable, disposable)

    return p
}


/**
 * 忽略的文件或者后缀
 */
class IgFileList : JBList<String>() {

    init {
        cellRenderer = DefaultListCellRenderer()
        model = DefaultListModel<String?>().apply {
            addAll(GenerateAssetsClassConfig.getGenerateAssetsSetting().igFiles)
        }
        border = BorderFactory.createLineBorder(JBColor.border())
        emptyText.appendText(PluginBundle.get("g.12"))
    }

    fun addItemString(name: String) {
        (model as DefaultListModel).addElement(name)
        GenerateAssetsClassConfig.getGenerateAssetsSetting().igFiles.add(name)
    }

    fun removeItem(name: String) {
        (model as DefaultListModel).removeElement(name)
        GenerateAssetsClassConfig.getGenerateAssetsSetting().igFiles.remove(name)
    }

}

