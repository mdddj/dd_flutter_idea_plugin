package shop.itbug.fluttercheckversionx.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.observable.util.addMouseHoverListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.*
import com.intellij.ui.hover.HoverListener
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import vm.VmService
import vm.element.FlutterInspectorGetPropertiesResponse
import vm.element.WidgetNode
import vm.element.WidgetTreeResponse
import vm.getProperties
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

// flutter 原生 widget tree
class FlutterWidgetTreeWidget(val project: Project, val group: String) : Disposable,
    Tree(DefaultTreeModel(DefaultMutableTreeNode("Waiting for widget data..."))) {

    private val myTreeModel
        get() = model as DefaultTreeModel
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var vmService: VmService? = null
    var isolateId: String? = null


    val clickListen = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2) { // 双击
                val path = getPathForLocation(e.x, e.y)
                if (path != null) {
                    val node = path.lastPathComponent as DefaultMutableTreeNode
                    val userObject = node.userObject
                    if (userObject is WidgetNode) {
                        handleWidgetNodeClick(userObject)
                    }
                }
            }
        }
    }

    init {

        border = emptyBorder()
        SmartExpander.installOn(this)
        TreeUIHelper.getInstance().installTreeSpeedSearch(this)
        isRootVisible = true
        isHorizontalAutoScrollingEnabled = false
        isOpaque = false
        this.cellRenderer = WidgetTreeCellRenderer()

        addMouseListener(clickListen)

    }

    // 更新 widget tree
    fun updateTree(widgetTree: WidgetTreeResponse) {
        val response = widgetTree
        SwingUtilities.invokeLater {
            response.result?.let { rootWidget ->
                val rootNode = createTreeNodes(rootWidget)
                myTreeModel.setRoot(rootNode)
                repaint()
                invalidate()
                updateUI()
                expandTree(3)
            }
                ?: run {
                    myTreeModel.setRoot(DefaultMutableTreeNode("Failed to parse widget data."))
                }
        }
    }

    /** 递归函数，将我们的 WidgetNode 转换为 Swing 的 TreeNode */
    private fun createTreeNodes(widgetNode: WidgetNode): DefaultMutableTreeNode {
        val treeNode = DefaultMutableTreeNode(widgetNode)
        widgetNode.children?.forEach { childWidget -> treeNode.add(createTreeNodes(childWidget)) }
        return treeNode
    }

    private fun expandTree(level: Int) {
        for (i in 0 until level) {
            expandRow(i)
        }
    }

    /** 处理 Widget 节点点击事件 */
    private fun handleWidgetNodeClick(widgetNode: WidgetNode) {
        val nodeId = widgetNode.valueId
        if (nodeId != null && vmService != null && isolateId != null) {
            scope.launch {
                try {
                    val response = vmService!!.getProperties(isolateId!!, group, nodeId)

                    // 在 EDT 线程中处理 UI 操作
                    ApplicationManager.getApplication().invokeLater { openSourceFile(response) }
                } catch (e: Exception) {
                    println("获取 widget 属性失败: ${e.message}")
                }
            }
        }
    }

    /** 从属性响应中提取文件信息并打开源码文件 */
    private fun openSourceFile(response: FlutterInspectorGetPropertiesResponse) {
        try {
            // 尝试从响应中提取 creationLocation 信息
            val creationLocation = response.result.find { it.creationLocation != null }?.creationLocation

            if (creationLocation != null) {
                val file = creationLocation.file
                val line = creationLocation.line
                val column = creationLocation.column

                // 将文件路径转换为本地文件系统路径
                val localFile =
                    if (file.startsWith("file://")) {
                        File(file.substring(7))
                    } else {
                        File(file)
                    }

                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(localFile)
                if (virtualFile != null) {
                    val editors =
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)

                    // 跳转到指定行和列
                    if (editors.isNotEmpty()) {
                        val editor =
                            FileEditorManager.getInstance(project).selectedTextEditor
                        editor?.let {
                            val offset =
                                it.document.getLineStartOffset(maxOf(0, line - 1)) +
                                        maxOf(0, column - 1)
                            it.caretModel.moveToOffset(minOf(offset, it.document.textLength))
                            it.scrollingModel.scrollToCaret(
                                com.intellij.openapi.editor.ScrollType.CENTER
                            )
                        }
                    }
                } else {
                    println("无法找到文件: $file")
                }
            } else {
                println("响应中没有找到 creationLocation 信息")
            }
        } catch (e: Exception) {
            println("打开源码文件失败: ${e.message}")
        }
    }

    /** 设置 VM 服务实例和隔离区 ID */
    fun setVmService(vmService: VmService?, isolateId: String?) {
        this.vmService = vmService
        this.isolateId = isolateId
    }

    override fun dispose() {
        removeMouseListener(clickListen)
    }

    inner class WidgetTreeCellRenderer : ColoredTreeCellRenderer() {

        val hoverListen = object : HoverListener() {
            override fun mouseEntered(p0: Component, p1: Int, p2: Int) {
                val comp = getComponentAt(p1, p2) as? JComponent
            }

            override fun mouseMoved(p0: Component, p1: Int, p2: Int) {

            }

            override fun mouseExited(p0: Component) {
            }

        }

        override fun customizeCellRenderer(
            p0: JTree,
            value: Any?,
            p2: Boolean,
            p3: Boolean,
            p4: Boolean,
            p5: Int,
            p6: Boolean
        ) {
            addMouseHoverListener(p0 as FlutterWidgetTreeWidget, hoverListen)
            if (value is DefaultMutableTreeNode) {
                val userObject = value.userObject

                if (userObject is WidgetNode) {
                    // 如果是我们的 WidgetNode 对象，就自定义显示内容
                    append("${userObject.description}", SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES)

                    // 设置一个默认图标
                    icon = AllIcons.Nodes.Class

                    // 如果该 Widget 不是由本地项目创建的（例如 Flutter 框架自带的），用灰色显示以区分
                    if (userObject.createdByLocalProject != true) {
                        foreground = JBColor.GRAY
                    }

                    toolTipText = "<${userObject.widgetRuntimeType}>"

                } else {
                    // 对于根提示节点或其他非 WidgetNode 的节点，正常显示
                    append(userObject.toString())
                    icon = AllIcons.Nodes.Folder
                }
            }
        }
    }


}
