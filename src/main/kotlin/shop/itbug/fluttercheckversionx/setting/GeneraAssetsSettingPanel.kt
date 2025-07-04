package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
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
import javax.swing.*


typealias GeneraAssetsSettingPanelIsModified = (value: Boolean) -> Unit

/**
 * 生成资产文件的设置面板
 */
class GeneraAssetsSettingPanel(
    val project: Project,
    var settingModel: GenerateAssetsClassConfigModel,
    val parentDisposable: Disposable,
    modified: GeneraAssetsSettingPanelIsModified,

    ) : BorderLayoutPanel(), Disposable {

    //忽略的文件
    private val igFilesWidget = IgFileList(project).apply {
        border = BorderFactory.createEmptyBorder()
    }


    private val dialogPanel = getGeneraAssetsPanel(project, settingModel, this, modified)


    init {
        addToCenter(createTopActionsPanel())
        addToRight(createRightSettingPanel())
        Disposer.register(parentDisposable, this)
    }

    private fun createTopActionsPanel(): JPanel {
        return ToolbarDecorator.createDecorator(igFilesWidget).setAddAction {
            WidgetUtil.configTextFieldModal(
                project = project, labelText = PluginBundle.get("g.13"), comment = "eg: test.json"
            ) {
                this@GeneraAssetsSettingPanel.igFilesWidget.addItemString(it)
            }
        }.setRemoveAction {
            val selectedValue = igFilesWidget.selectedValue
            if (selectedValue.isNotEmpty()) {
                this@GeneraAssetsSettingPanel.igFilesWidget.removeItem(selectedValue)
            }
        }.addExtraAction(
            WidgetUtil.getHelpAnAction { it ->
                it.inputEvent?.let {
                    WidgetUtil.showTopBalloon(it.component, PluginBundle.get("g.14"))
                }

            },
        ).createPanel()

    }

    private fun createRightSettingPanel() = BorderLayoutPanel().apply {
        addToCenter(dialogPanel)
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
    }


    fun doApply() {
        dialogPanel.apply()
    }

    override fun dispose() {

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
            comment(Links.generateDocCommit(Links.ASSETS))
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

    val newDisposable = Disposer.newDisposable(parentDisposable)
    val alarm = Alarm(newDisposable)


    fun initValidation() {
        alarm.addRequest({
            isModified.invoke(p.isModified())
            initValidation()
        }, 400)
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
        cellRenderer = ItemRender()
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

private class ItemRender : ColoredListCellRenderer<String>() {
    override fun customizeCellRenderer(
        p0: JList<out String?>,
        p1: String?,
        p2: Int,
        p3: Boolean,
        p4: Boolean
    ) {
        p1?.let {
            append(it)
        }
    }
}