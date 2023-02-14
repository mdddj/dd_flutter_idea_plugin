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
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.vcs.commit.NonModalCommitPanel.Companion.showAbove
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities


typealias GeneraAssetsSettingPanelIsModified = (value: Boolean) -> Unit

/**
 * 生成资产文件的设置面板
 */
class GeneraAssetsSettingPanel(var settingModel: GenerateAssetsClassConfigModel, val parentDisposable: Disposable,val modified: GeneraAssetsSettingPanelIsModified) :
    BorderLayoutPanel() {

    //忽略的文件
    private val igFilesWidget = IgFileList()


    init {
        addToTop(createToolBar())
        addToCenter(igFilesWidget)
        addToRight(createRightSettingPanel())
    }

    private fun createRightSettingPanel() = BorderLayoutPanel().apply {
        addToCenter(getGeneraAssetsPanel(settingModel, parentDisposable,modified))
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

        val rightToolbar = ActionManager.getInstance()
            .createActionToolbar("GenerateAssetsIgFileRightToolbar", DefaultActionGroup(*createRightActions()), true)
        addToRight(rightToolbar.component)
    }

    private fun createRightActions(): Array<AnAction> = arrayOf(
        //帮助图标
        object : DumbAwareAction(AllIcons.Actions.Help) {
            override fun actionPerformed(e: AnActionEvent) {

            }
        }
    )

    private fun createActions(): Array<AnAction> = arrayOf(
        //添加
        object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                WidgetUtil.getTextEditorPopup("输入要忽略的文件", "", {
                    it.showAbove(igFilesWidget)
                }) {
                    this@GeneraAssetsSettingPanel.igFilesWidget.addItemString(it)
                }
            }
        },
        //删除
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
        })
}


fun getGeneraAssetsPanel(settingModel: GenerateAssetsClassConfigModel, parentDisposable: Disposable, modified: GeneraAssetsSettingPanelIsModified): DialogPanel {


    val p: DialogPanel = panel {
        row("类名") {
            textField().bindText({ settingModel.className }, {
                GenerateAssetsClassConfig.getGenerateAssetsSetting().className = it
            })
        }
        row("文件名") {
            textField().bindText({ settingModel.fileName }, {
                GenerateAssetsClassConfig.getGenerateAssetsSetting().fileName = it
            })
        }
        row("保存路径") {
            textField().bindText({ settingModel.path }, {
                GenerateAssetsClassConfig.getGenerateAssetsSetting().path = it
            })
        }
        row("替换字符") {
            textField().bindText(settingModel::replaceTags)
        }.contextHelp("如果文件名中包含这些特殊字符,将会自动替换成下换线_", "替换字符")
        row("命名规范") {
            checkBox("属性值首字母大写").bindSelected(settingModel::firstChatUpper)
        }
        row("命名前缀") {
            checkBox("命名添加文件夹路径").bindSelected(settingModel::addFolderNamePrefix)
        }
        row("命名后缀") {
            checkBox("命名添加文件类型后缀").bindSelected(settingModel::addFileTypeSuffix)
                .comment("感谢尘定同学提出的建议", 11)
        }
        row("弹窗提醒") {
            checkBox("每次生成不需要弹窗").bindSelected(settingModel::dontTip)
        }
        row("监听变化") {
            checkBox("文件更改后自动生成").bindSelected(settingModel::autoListenFileChange)
        }
    }


    val alarm = Alarm(parentDisposable)


    fun initValidation() {
        alarm.addRequest({
            if (p.isModified()) {
                p.apply()

            }
            modified.invoke(p.isModified())
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
        emptyText.appendText("暂无忽略&过滤的文件")
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

