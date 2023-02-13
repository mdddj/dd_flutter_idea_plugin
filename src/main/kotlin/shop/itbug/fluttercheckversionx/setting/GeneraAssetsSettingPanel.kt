package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBList
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.widget.askString
import java.awt.BorderLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel

/**
 * 生成资产文件的设置
 */
class GeneraAssetsSettingPanel : BorderLayoutPanel() {


    private val igFilesWidget = IgFileList()

    init {
        addToTop(createToolBar())
        igFilesWidget.addItemString("test.png")
        add(igFilesWidget, BorderLayout.CENTER)
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
        object : DumbAwareAction(MyIcons.add) {
            override fun actionPerformed(e: AnActionEvent) {
                e.project?.askString(this@GeneraAssetsSettingPanel.igFilesWidget::addItemString)
            }
        }
    )
}

/**
 * 忽略的文件或者后缀
 */
class IgFileList : JBList<String>() {

    init {
        cellRenderer = DefaultListCellRenderer()
        model = DefaultListModel()

    }

    fun addItemString(name: String) {
        (model as DefaultListModel).addElement(name)
    }


}

