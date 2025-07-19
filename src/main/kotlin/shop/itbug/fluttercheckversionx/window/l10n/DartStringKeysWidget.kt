package shop.itbug.fluttercheckversionx.window.l10n

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.services.DartString
import shop.itbug.fluttercheckversionx.services.FlutterL10nService
import shop.itbug.fluttercheckversionx.services.FlutterL10nService.OnDartStringScanCompletedListener
import shop.itbug.fluttercheckversionx.services.group
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode

/// dart string keys
class DartStringKeysTree(val project: Project) : DnDAwareTree(DefaultMutableTreeNode("Dart String Elements")),
    Disposable,
    OnDartStringScanCompletedListener, TreeSelectionListener {

    private val service = FlutterL10nService.getInstance(project)

    init {
        initDataModel()
        Disposer.register(service, this)
        project.messageBus.connect(this).subscribe(FlutterL10nService.OnDartStringScanCompleted, this)
        addTreeSelectionListener(this)
    }

    private fun initDataModel() {
        SwingUtilities.invokeLater {
            val dartStrings = service.dartStringList.group()
            dartStrings.forEach { (key, value) ->
                val treeNode = DefaultMutableTreeNode(key.name)
                value.forEach {
                    treeNode.add(DefaultMutableTreeNode(it))
                }
                getTreeModel().add(treeNode)
            }
        }
    }

    fun getTreeModel() = model.root as DefaultMutableTreeNode

    override fun dispose() {
        removeTreeSelectionListener(this)
    }

    override fun onDartStringScanCompleted(
        project: Project,
        list: List<DartString>,
        group: Map<VirtualFile, List<DartString>>
    ) {
        SwingUtilities.invokeLater {
            initDataModel()
        }
    }

    override fun valueChanged(e: TreeSelectionEvent?) {
        e?.let {
            val root = model.root as DefaultMutableTreeNode
            if (root != lastSelectedPathComponent) {
                val last = lastSelectedPathComponent as DefaultMutableTreeNode? ?: return
                when (val userObj = last.userObject) {
                    is DartString -> {
                        val ele = runReadAction { userObj.element.element }
                        ele?.navigate(true)
                    }

                    else -> {
                        println("点击了:${last}")
                    }
                }
            }
        }
    }


}