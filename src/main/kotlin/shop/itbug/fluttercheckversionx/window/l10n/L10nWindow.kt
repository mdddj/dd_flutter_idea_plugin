package shop.itbug.fluttercheckversionx.window.l10n

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import shop.itbug.fluttercheckversionx.actions.context.HelpContextAction
import shop.itbug.fluttercheckversionx.actions.context.SiteDocument
import shop.itbug.fluttercheckversionx.actions.tool.FlutterL10nRunGenAction
import shop.itbug.fluttercheckversionx.actions.tool.FlutterL10nSettingChangeAction
import shop.itbug.fluttercheckversionx.actions.tool.FlutterL10nWindowTreeRefreshAction
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.*
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * l10n多语言窗口
 */
class L10nWindow(val project: Project, val toolWindow: ToolWindow) : BorderLayoutPanel(), Disposable,
    FlutterL10nService.OnL10nKeysChangedListener,
    TreeSelectionListener, FlutterL10nService.OnArbFileChangedListener, UiDataProvider {
    private val panel = MyPanel()
    private val editorContainer = JBScrollPane(panel)
    private val myTree = MyL10nKeysTree(project)
    private val service = FlutterL10nService.getInstance(project)
    private val sp = JBSplitter().apply {
        firstComponent = myTree.actionToolbar(project).apply {
            border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1)
        }
        secondComponent = editorContainer
    }


    init {
        project.messageBus.connect(this).subscribe(FlutterL10nService.ListenKeysChanged, this)
        project.messageBus.connect(this).subscribe(FlutterL10nService.ArbFileChanged, this)
        myTree.addTreeSelectionListener(this)
        editorContainer.border = emptyBorder()
        addToCenter(sp)
        SwingUtilities.invokeLater {
            initTreeModel()
        }
        Disposer.register(this, myTree)
    }


    private fun initTreeModel() {
        service.handleKeys {
            myTree.model = createTreeModel(it)
        }
    }


    override fun onKeysChanged(
        items: List<L10nKeyItem>, keysString: List<String>, project: Project
    ) {
        SwingUtilities.invokeLater {
            myTree.model = createTreeModel(keysString)
        }

    }


    fun getTree() = myTree

    private fun createTreeModel(keys: List<String>): DefaultTreeModel {
        val model = DefaultTreeModel(DefaultMutableTreeNode("l10n keys"))
        val root = model.root as DefaultMutableTreeNode
        keys.forEach {
            root.add(DefaultMutableTreeNode(it))
        }
        return model
    }

    fun changeEditorPanel(key: String) {
        panel.components.filterIsInstance<FlutterL10nKeyEditPanel>().forEach {
            Disposer.dispose(it)
        }
        panel.removeAll()
        val arbFiles = service.arbFiles
        arbFiles.forEach {
//            panel.add(editorPanel(it, key, this))
            panel.add(FlutterL10nKeyEditPanel(it, key, myTree, this))
        }
        panel.revalidate()
        panel.repaint()
    }

    override fun dispose() {
        myTree.removeTreeSelectionListener(this)
    }

    override fun valueChanged(e: TreeSelectionEvent?) {
        val last = myTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val isRoot = last.root == last
        if (isRoot) {
            panel.removeAll()
            panel.revalidate()
            panel.repaint()
            return
        }
        val obj = last.userObject as? String ?: return
        changeEditorPanel(obj)
    }

    override fun onArbFileChanged(arbFile: ArbFile) {
        service.handleKeys {
            SwingUtilities.invokeLater {
                myTree.model = createTreeModel(it)
            }
        }
        service.runFlutterGenL10nCommand()
    }

    override fun uiDataSnapshot(sink: DataSink) {

    }

    class MyPanel() : JBPanel<MyPanel>(VerticalLayout(12))
}


///树
class MyL10nKeysTree(val project: Project) : DnDAwareTree(DefaultMutableTreeNode()), UiDataProvider, Disposable,
    FlutterL10nService.OnL10nKeysChangedListener {
    private val service = FlutterL10nService.getInstance(project)
    private val arbFiles get() = service.arbFiles
    private val config get() = PluginConfig.getInstance(project).state
    private val defaultFileName get() = config.l10nDefaultFileName
    private val defaultUseArbFile get() = arbFiles.find { it.file.name == defaultFileName }

    init {
        TreeUIHelper.getInstance().installTreeSpeedSearch(this)
        SmartExpander.installOn(this)
        isRootVisible = true
        isHorizontalAutoScrollingEnabled = false
        isOpaque = false
        emptyText.text = PluginBundle.get("l10n.empty.text")
        border = emptyBorder()
        TreeUtil.installActions(this)
        CustomizationUtil.installPopupHandler(
            this, "flutter-l10n-right-menu", "Flutter l10n"
        )
        this.cellRenderer = Render()
        project.messageBus.connect(this).subscribe(FlutterL10nService.ListenKeysChanged, this)
    }

    fun selectValue(): String? {
        val last = lastSelectedPathComponent as? DefaultMutableTreeNode ?: return null
        if (last == last.root) return null
        return last.userObject as? String
    }

    override fun uiDataSnapshot(sink: DataSink) {
    }

    override fun dispose() {

    }

    override fun onKeysChanged(
        items: List<L10nKeyItem>,
        keysString: List<String>,
        project: Project
    ) {
        SwingUtilities.invokeLater {
            updateUI()
        }

    }

    inner class Render : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val key = value?.toString() ?: return
            append(key, SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES)
            val useArbFile = defaultUseArbFile
            useArbFile?.let { arbFile ->
                val find = arbFile.keyItems.find { it.key == key }
                if (find != null) {
                    appendTextPadding(12, SwingConstants.CENTER)
                    append("  " + find.value, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES, 12, SwingConstants.BOTTOM)
                }
            }


        }
    }

    companion object {
    }
}


///键编辑区域
class FlutterL10nKeyEditPanel(
    val arbFile: ArbFile,
    val key: String,
    val tree: MyL10nKeysTree,
    parentDisposable: Disposable
) :
    BorderLayoutPanel(),
    UiDataProvider, Disposable, DocumentListener {
    val vf = arbFile.file
    val project = arbFile.project

    val textArea = LanguageTextField(PlainTextLanguage.INSTANCE, project, arbFile.readValue(key)).apply {
        this.preferredSize = Dimension(preferredSize.width, 50)
    }

    val actionGroup = ActionManager.getInstance().getAction("FlutterL10nEditorPanelActionGroup") as DefaultActionGroup
    val toolbar = ActionManager.getInstance().createActionToolbar("L10nEditPanel", actionGroup, true).apply {
        targetComponent = this@FlutterL10nKeyEditPanel
    }
    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel(vf.name), toolbar.component)
        .addComponent(textArea)
        .panel


    init {
        Disposer.register(parentDisposable, this)
        addToCenter(panel)
        textArea.addDocumentListener(this)
        PopupHandler.installPopupMenu(this, "FlutterL10nEditorPanelActionGroup", "L10nEditPanel")
    }


    override fun uiDataSnapshot(sink: DataSink) {

    }

    override fun dispose() {
        textArea.removeDocumentListener(this)
    }

    fun reWriteTextToFile() {
        val text = textArea.text
        FlutterL10nService.getInstance(project).runWriteThread {
            arbFile.reWriteKeyValue(key, text)
        }
    }

    override fun documentChanged(event: DocumentEvent) {
        super.documentChanged(event)
        reWriteTextToFile()
    }

}


private fun MyL10nKeysTree.actionToolbar(project: Project): JPanel {
    val panel = ToolbarDecorator.createDecorator(this)
        .setAddAction {
            WidgetUtil.configTextFieldModal(
                project, PluginBundle.get("l10n.addDialog.labelText"), "It inserts this key into all ARB files"
            ) {
                FlutterL10nService.getInstance(project).insetNewKey(it)
            }
        }.addExtraAction(FlutterL10nWindowTreeRefreshAction.getAction())
        .addExtraAction(FlutterL10nSettingChangeAction.getInstance())
        .addExtraAction(FlutterL10nRunGenAction.getInstance())
        .addExtraAction(HelpContextAction.ACTION)
        .setPanelBorder(emptyBorder())
        .setToolbarBorder(emptyBorder()).setScrollPaneBorder(emptyBorder())
        .createPanel()
    panel.border = emptyBorder()
    putUserData(HelpContextAction.DataKey, SiteDocument.L10n)
    return panel
}


