package shop.itbug.fluttercheckversionx.window.l10n

import com.intellij.icons.AllIcons
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.*
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.plugins.gradle.util.TextIcon
import shop.itbug.fluttercheckversionx.actions.tool.FlutterL10nSettingChangeAction
import shop.itbug.fluttercheckversionx.actions.tool.FlutterL10nWindowTreeRefreshAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.*
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * l10n多语言窗口
 */
class L10nWindow(val project: Project) : BorderLayoutPanel(), Disposable, FlutterL10nService.OnL10nKeysChangedListener,
    TreeSelectionListener, FlutterL10nService.OnArbFileChangedListener, UiDataProvider {
    val panel = MyPanel()
    val editorContainer = JBScrollPane(panel)
    val myTree = MyL10nKeysTree(project)
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
        myTree.border = emptyBorder()
        myTree.addTreeSelectionListener(this)
        editorContainer.border = emptyBorder()
        addToCenter(sp)
        SwingUtilities.invokeLater {
            initTreeModel()
        }


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


    fun createTreeModel(keys: List<String>): DefaultTreeModel {
        val model = DefaultTreeModel(DefaultMutableTreeNode("l10n keys"))
        val root = model.root as DefaultMutableTreeNode
        keys.forEach {
            root.add(DefaultMutableTreeNode(it))
        }
        return model
    }

    fun changeEditorPanel(key: String) {
        panel.removeAll()
        val arbFiles = service.arbFiles
        arbFiles.forEach {
            panel.add(editorPanel(it, key, this))
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
class MyL10nKeysTree(project: Project) : DnDAwareTree(DefaultMutableTreeNode()), UiDataProvider {

    init {
        TreeUIHelper.getInstance().installTreeSpeedSearch(this)
        cellRenderer = L10nTreeCellRender()
        SmartExpander.installOn(this)
        isRootVisible = true
        isHorizontalAutoScrollingEnabled = false
        isOpaque = false
        emptyText.text = PluginBundle.get("l10n.empty.text")
        TreeUtil.installActions(this)
        CustomizationUtil.installPopupHandler(
            this, "flutter-l10n-right-menu", "Flutter l10n"
        )
    }

    override fun uiDataSnapshot(sink: DataSink) {
    }


    private inner class L10nTreeCellRender : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
        ) {
            val key = value as? String? ?: return
            icon = TextIcon(key[0].toString())
            append(key)
        }
    }

    companion object {
    }
}


//编辑区域
private fun editorPanel(arbFile: ArbFile, key: String, parentDisposable: Disposable): DialogPanel {
    val vf = arbFile.file
    val project = arbFile.project
    var valueString = arbFile.readValue(key)


    val showInProjectViewAction = object : DumbAwareAction("Show In Project View", "", AllIcons.General.Locate) {
        override fun actionPerformed(e: AnActionEvent) {
            ProjectView.getInstance(project).select(null, vf, true)
        }
    }
    val openFileAction = object : DumbAwareAction("Open File", "", AllIcons.General.Show) {
        override fun actionPerformed(e: AnActionEvent) {
            val editorArr = FileEditorManager.getInstance(project).openFile(vf, true)
            if (editorArr.isNotEmpty()) {
                val edit = editorArr.first() as? PsiAwareTextEditorImpl ?: return
                arbFile.moveToOffset(key, edit.editor)
            }
        }
    }


    val myPanel = panel {
        row {
            label(vf.name).component.apply {
                font = JBFont.label().deriveFont(Font.BOLD)
            }
            actionButton(openFileAction)
            actionButton(showInProjectViewAction)
        }
        row {
            textArea().rows(4).align(Align.FILL).bindText({ valueString }, { valueString = it })
        }

    }


    val newDisposable = Disposer.newDisposable()
    Disposer.register(parentDisposable, newDisposable)
    myPanel.registerValidators(newDisposable)
    val alarm = Alarm(newDisposable)
    fun addListenValueChange() {
        alarm.addRequest({
            if (myPanel.isModified()) {
                myPanel.apply()
                FlutterL10nService.getInstance(project).runWriteThread {
                    arbFile.reWriteKeyValue(key, valueString)
                }
            }
            addListenValueChange()
        }, 1000)

    }
    SwingUtilities.invokeLater {
        addListenValueChange()
    }


    return myPanel
}


private fun MyL10nKeysTree.actionToolbar(project: Project): JPanel {
    val panel = ToolbarDecorator.createDecorator(this).setAddAction {
        WidgetUtil.configTextFieldModal(
            project, PluginBundle.get("l10n.addDialog.labelText"), "It inserts this key into all ARB files"
        ) {
            FlutterL10nService.getInstance(project).insetNewKey(it)
        }
    }.addExtraAction(FlutterL10nWindowTreeRefreshAction.getAction())
        .addExtraAction(FlutterL10nSettingChangeAction.getInstance()).setPanelBorder(emptyBorder())
        .setToolbarBorder(emptyBorder()).setScrollPaneBorder(emptyBorder()).createPanel()
    panel.border = emptyBorder()

    return panel
}