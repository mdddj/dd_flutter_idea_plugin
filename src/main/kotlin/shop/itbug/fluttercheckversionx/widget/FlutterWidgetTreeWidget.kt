package shop.itbug.fluttercheckversionx.widget

import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.json.JsonFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.*
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.tools.emptyBorder
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import vm.*
import vm.element.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class FlutterTreeComponent(val project: Project, val group: String, val vmService: VmService) : BorderLayoutPanel(),
    UiDataProvider,
    Disposable {
    private val tree = FlutterWidgetTreeWidget(project, group, vmService)
    private val toolbar = ActionManager.getInstance().createActionToolbar("FlutterTree", createActionsGroup(), true)

    init {
        toolbar.targetComponent = this
        Disposer.register(this, tree)
        addToCenter(tree)
        addToTop(toolbar.component)
    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[FlutterWidgetTreeWidget.TREE_WIDGET] = tree
    }

    override fun dispose() {

    }

    fun isEq(service: VmService): Boolean = vmService == service


    fun createActionsGroup(): DefaultActionGroup {
        return ActionManager.getInstance().getAction("FlutterVMTreeToolbarAction") as DefaultActionGroup
    }

}

// flutter 原生 widget tree
class FlutterWidgetTreeWidget(val project: Project, val group: String, val vmService: VmService) : Disposable,
    Tree(DefaultTreeModel(DefaultMutableTreeNode("Waiting for widget data..."))),
    InspectorStateManager.InspectorStateListener, UiDataProvider {

    private val myTreeModel
        get() = model as DefaultTreeModel
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var isolateId: String? = null
    private val listenStreams = arrayOf(EventKind.Extension.name, EventKind.ToolEvent.name)

    // 存储当前的WidgetTreeResponse，用于导出JSON
    var currentWidgetTreeResponse: WidgetTreeResponse? = null
        private set

    // 是否启用详细模式（显示Text内容等）
    var detailedMode: Boolean = false  // 默认关闭详细模式以避免嵌套问题

    // Inspector状态管理器
    private var inspectorStateManager: InspectorStateManager? = null


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
        SwingUtilities.invokeLater {
            refreshTree()
        }
        scope.launch {
            listenStreams.forEach(vmService::streamListen)
        }

    }


    fun refreshTree(isInit: Boolean = true) {
        scope.launch {
            if (isInit) {
                delay(800)
            }

            val isolateId = vmService.getMainIsolateId()
            if (isolateId.isNotBlank()) {
                if (inspectorStateManager == null) {
                    inspectorStateManager = InspectorStateManager.getOrCreate(vmService, isolateId)
                    inspectorStateManager!!.addStateListener(this@FlutterWidgetTreeWidget)
                }
                try {
                    val rootTree = if (detailedMode) {
                        // 使用详细模式获取Text内容和其他详细信息
                        vmService.getDetailedWidgetTree(isolateId, group)
                    } else {
                        // 使用简单模式
                        vmService.getRootWidgetTree(isolateId, group)
                    }

                    if (rootTree != null) {
                        // 限制树的深度以避免嵌套过深
                        val limitedTree = rootTree.limitDepth(30) // 限制最大深度为30
                        updateTree(limitedTree)
                    }
                } catch (e: Exception) {
                    println("获取Widget Tree失败: ${e.message}")
                    // 如果失败，尝试使用最简单的模式
                    try {
                        val simpleTree = vmService.getRootWidgetTree(
                            isolateId = isolateId,
                            groupName = group,
                            isSummaryTree = true,
                            withPreviews = false,
                            fullDetails = false
                        )
                        if (simpleTree != null) {
                            updateTree(simpleTree.limitDepth(20))
                        }
                    } catch (fallbackException: Exception) {
                        println("获取简单Widget Tree也失败: ${fallbackException.message}")
                        SwingUtilities.invokeLater {
                            myTreeModel.setRoot(DefaultMutableTreeNode("Failed to load widget tree: ${fallbackException.message}"))
                        }
                    }
                }
            }


        }
    }

    // 更新 widget tree
    fun updateTree(widgetTree: WidgetTreeResponse) {
        val response = widgetTree
        // 保存当前的WidgetTreeResponse
        currentWidgetTreeResponse = response

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
        if (nodeId != null && isolateId != null) {
            scope.launch {
                try {
                    val response = vmService.getProperties(isolateId!!, group, nodeId)
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
                MyFileUtil.openFile(project, file, line, column)
            }
        } catch (e: Exception) {
            println("打开源码文件失败: ${e.message}")
        }
    }


    /**
     * 在Editor区域打开当前WidgetTreeResponse的JSON文本
     */
    fun openWidgetTreeJsonInEditor() {
        val response = currentWidgetTreeResponse
        if (response == null) {
            println("No widget tree data available")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            try {
                // 使用Gson格式化JSON
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonText = gson.toJson(response)

                // 创建虚拟文件
                val virtualFile = LightVirtualFile(
                    "widget_tree_${System.currentTimeMillis()}.json",
                    JsonFileType.INSTANCE,
                    jsonText
                )

                // 在编辑器中打开文件
                FileEditorManager.getInstance(project).openFile(virtualFile, true)

                println("Widget tree JSON opened in editor")
            } catch (e: Exception) {
                println("Failed to open widget tree JSON: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取Inspector状态管理器
     */
    fun getInspectorStateManager(): InspectorStateManager? = inspectorStateManager


    override fun dispose() {
        scope.launch {
            listenStreams.forEach(vmService::streamCancel)
        }
        removeMouseListener(clickListen)

        // 清理Inspector状态管理器
        if (isolateId != null) {
            InspectorStateManager.cleanup(vmService, isolateId!!)
        }
        inspectorStateManager?.removeStateListener(this)
        inspectorStateManager = null
    }

    override fun onOverlayStateChanged(enabled: Boolean) {
    }

    override fun navigate(result: NavigatorLocationInfo) {
        MyFileUtil.openFile(project, result.fileUri, result.line, result.column)
    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[TREE_WIDGET] = this
    }

    companion object {
        val TREE_WIDGET = DataKey.create<FlutterWidgetTreeWidget>("flutter_widget_tree")
    }

    inner class WidgetTreeCellRenderer : ColoredTreeCellRenderer() {


        override fun customizeCellRenderer(
            p0: JTree,
            value: Any?,
            p2: Boolean,
            p3: Boolean,
            p4: Boolean,
            p5: Int,
            p6: Boolean
        ) {
            if (value is DefaultMutableTreeNode) {
                val userObject = value.userObject

                if (userObject is WidgetNode) {
                    // 显示widget描述
                    append("${userObject.description}", SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES)

                    // 如果有textPreview（Text widget的文本内容），显示它
                    if (!userObject.textPreview.isNullOrBlank()) {
                        append(" ", SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES)
                        append("\"${userObject.textPreview}\"", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }

                    // 设置图标
                    icon = when (userObject.widgetRuntimeType) {
                        "Text" -> AllIcons.FileTypes.Text
                        "Container" -> AllIcons.Nodes.Package
                        "Column", "Row" -> AllIcons.Actions.SplitVertically
                        else -> AllIcons.Nodes.Class
                    }

                    // 如果该 Widget 不是由本地项目创建的（例如 Flutter 框架自带的），用灰色显示以区分
                    if (userObject.createdByLocalProject != true) {
                        foreground = JBColor.GRAY
                    }

                    // 构建更详细的tooltip
                    val tooltipBuilder = StringBuilder()
                    tooltipBuilder.append("<html>")
                    tooltipBuilder.append("<b>${userObject.widgetRuntimeType}</b><br>")
                    if (!userObject.textPreview.isNullOrBlank()) {
                        tooltipBuilder.append("Text: \"${userObject.textPreview}\"<br>")
                    }
                    if (userObject.properties?.isNotEmpty() == true) {
                        tooltipBuilder.append("<br><b>Properties:</b><br>")
                        userObject.properties.take(5).forEach { prop ->
                            tooltipBuilder.append("${prop.name}: ${prop.value}<br>")
                        }
                    }
                    tooltipBuilder.append("</html>")
                    toolTipText = tooltipBuilder.toString()

                } else {
                    // 对于根提示节点或其他非 WidgetNode 的节点，正常显示
                    append(userObject.toString())
                    icon = AllIcons.Nodes.Folder
                }
            }
        }
    }


}



