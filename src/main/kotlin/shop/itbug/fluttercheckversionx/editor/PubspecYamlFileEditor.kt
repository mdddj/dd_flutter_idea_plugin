package shop.itbug.fluttercheckversionx.editor

import com.intellij.diff.util.FileEditorBase
import com.intellij.icons.AllIcons
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.ui.TreeModel
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.popup.HintUpdateSupply
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.MyPackageGroup
import shop.itbug.fluttercheckversionx.services.PubPackage
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MyYamlSplitEditor(private val textEditor: TextEditor, private val visualComponent: JComponent) :
    FileEditorBase() {

    private val splitPane = JBSplitter(false).apply {
        firstComponent = textEditor.component
        secondComponent = visualComponent
        splitterProportionKey = "FlutterxSplitterProportionKey"
    }

    override fun getComponent(): JComponent = splitPane
    override fun getName(): String = "FlutterX"
    override fun getPreferredFocusedComponent(): JComponent? {
        return textEditor.preferredFocusedComponent
    }

    override fun getFile(): VirtualFile = textEditor.file

}

class MyYamlSplitEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension == "yaml" && file.name == "pubspec.yaml"
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val visualComponent = MyDartPackageTree.createPanel(project)
        return MyYamlSplitEditor(textEditor, visualComponent)
    }

    override fun getEditorTypeId(): String = "flutterx-yaml-split-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}


class MyDartPackageTree(val project: Project) : DnDAwareTree(), DartPackageCheckService.FetchDartPackageFinish {

    private val packageService = DartPackageCheckService.getInstance(project)

    init {
        emptyText.text = ""
        model = TreeModel(DefaultMutableTreeNode())
        isLargeModel = true
        cellRenderer = TreeRender(project)
        HintUpdateSupply.installDataContextHintUpdateSupply(this)
        project.messageBus.connect().subscribe(DartPackageCheckService.FetchDartPackageFinishTopic, this)
        SwingUtilities.invokeLater {
            finish(packageService.details)
        }
    }

    override fun finish(details: List<PubPackage>) {
        val map = packageService.details.groupBy { it.first.group }
        val root = DefaultMutableTreeNode(packageService.projectName)
        map.forEach {
            val r = DefaultMutableTreeNode(it.key)
            it.value.forEach { item ->
                r.add(DefaultMutableTreeNode(item))
            }
            root.add(r)
        }
        this.model = DefaultTreeModel(root)

        (model as DefaultTreeModel).apply {
            nodeChanged(root)
            reload()
        }
        repaint()
    }


    companion object {

        fun createPanel(project: Project): JPanel {
            val panel = ToolbarDecorator.createDecorator(MyDartPackageTree(project)).apply {
                    disableAddAction()
                    disableRemoveAction()
                    disableUpDownActions()
                    disableInputMethodSupport()
                    addExtraAction(object : AnAction({ "Refresh" }, AllIcons.Actions.Refresh) {
                        override fun actionPerformed(e: AnActionEvent) {
                            DartPackageCheckService.getInstance(project).startResetIndex()
                        }
                    })
                }.setPanelBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)).createPanel()
            return panel
        }
    }

}

private class TreeRender(val project: Project) : NodeRenderer() {
    override fun customizeCellRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ) {
        val obj = (value as? DefaultMutableTreeNode)?.userObject
        val myIcon: Icon? = when (obj) {
            is String -> MyIcons.flutter
            is PubPackage -> MyIcons.dartPackageIcon
            is MyPackageGroup -> AllIcons.Nodes.Folder
            else -> null
        }
        val pubLastVersion = when (obj) {
            is PubPackage -> obj.second?.latest?.version
            else -> null
        }

        val canUpdate = when (obj) {
            is PubPackage -> obj.hasNew()
            else -> false
        }

        val updateDate = when (obj) {
            is PubPackage -> obj.getLastUpdateTime()
            else -> null
        }
        icon = myIcon
        append(obj.toString())
        pubLastVersion?.let {
            if (canUpdate) {
                append(
                    "${PluginBundle.get("pub_dart_package_new_version")}:$it",
                    SimpleTextAttributes.LINK_BOLD_ATTRIBUTES,
                    12,
                    SwingConstants.RIGHT
                )
            }

        }
        if (canUpdate) {
            append(
                PluginBundle.get("pub_package_has_new_version_tips"),
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
                12,
                SwingConstants.RIGHT
            )
        }


        updateDate?.let {
            append(PluginBundle.get("lastupdate_date_time") + ":" + it)
        }


    }


}