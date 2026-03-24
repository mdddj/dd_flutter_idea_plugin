package shop.itbug.flutterx.widget_tree

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.foundation.lazy.tree.buildTree
import vm.*
import vm.element.Property
import vm.element.WidgetNode
import java.util.*

class WidgetTreeViewModel(
    private val vmService: VmService,
    private val coroutineScope: CoroutineScope,
    private val project: com.intellij.openapi.project.Project
) {
     val logger = thisLogger()
    private val objectGroup = "flutterx_widget_tree_${UUID.randomUUID()}"
    private var currentIsolateId: String? = null
    // 状态流
    private val _treeState = MutableStateFlow<Tree<WidgetNode>?>(null)
    val treeState = _treeState.asStateFlow()
    
    // 原始树数据，用于切换模式时重构 jewel tree
    private var rawRootNode: WidgetNode? = null
    
    private val _isCompactMode = MutableStateFlow(true) // 默认开启紧凑模式
    val isCompactMode = _isCompactMode.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // 选中的节点
    private val _selectedNode = MutableStateFlow<WidgetNode?>(null)
    val selectedNode = _selectedNode.asStateFlow()
    
    // 选中节点的详细属性
    private val _selectedNodeProperties = MutableStateFlow<List<Property>>(emptyList())
    val selectedNodeProperties = _selectedNodeProperties.asStateFlow()
    
    // 属性加载状态
    private val _isLoadingProperties = MutableStateFlow(false)
    val isLoadingProperties = _isLoadingProperties.asStateFlow()

    // 搜索状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<Set<String>>(emptySet())
    val searchMatches = _searchMatches.asStateFlow()
    
    // 当前匹配项索引（用于导航）
    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex = _currentMatchIndex.asStateFlow()
    
    // 扁平化的匹配项列表（用于顺序导航和 Popup 展示）
    private val _matchedNodes = MutableStateFlow<List<WidgetNode>>(emptyList())
    val matchedNodes = _matchedNodes.asStateFlow()

    init {
        loadTree()
    }

    fun loadTree() {
        coroutineScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                // 先清理旧的 group (如果存在)
                currentIsolateId?.let { 
                     vmService.disposeGroup(it, objectGroup)
                }

                val isolateId = vmService.getMainIsolateId()
                if (isolateId.isEmpty()) {
                    _error.value = "No active isolate found. Please ensure the app is running."
                    return@launch
                }
                currentIsolateId = isolateId
                
                // 设置 PubRootDirectories 以获取 creationLocation
                val basePath = project.basePath
                if (basePath != null) {
                    vmService.setPubRootDirectories(isolateId, listOf(basePath))
                }
                
                // 获取 Widget Tree
                // 使用 summary tree, withPreviews=true 以在树中显示文本
                val response = vmService.getWidgetTree(
                    isolateId = isolateId, 
                    groupName = objectGroup,
                    isSummaryTree = true,
                    withPreviews = true,

                )
                
                if (response?.result != null) {
                    rawRootNode = response.result
                    refreshJewelTree()
                } else {
                    _error.value = "Failed to load widget tree or tree is empty."
                }
                
            } catch (e: Exception) {
                logger.error("Error loading widget tree", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 切换紧凑模式
     */
    fun toggleCompactMode() {
        _isCompactMode.value = !_isCompactMode.value
        refreshJewelTree()
    }

    private fun refreshJewelTree() {
        val root = rawRootNode ?: return
        _treeState.value = buildWidgetTree(root)
    }

    private fun buildWidgetTree(root: WidgetNode): Tree<WidgetNode> = buildTree {
        val key = root.valueId ?: "node_${System.identityHashCode(root)}"
        
        // 如果是紧凑模式，从根节点开始简化
        val processedNode = if (isCompactMode.value) findFirstProjectOrMultiChildNode(root) else root
        
        if (processedNode.children?.isNotEmpty() == true) {
            addNode(processedNode, id = key) {
                processedNode.children.forEach { child ->
                    appendChildNodeWithSimplification(child)
                }
            }
        } else {
            addLeaf(processedNode, id = key)
        }
    }

    /**
     * 在紧凑模式下，寻找第一个“有意义”的节点：
     * 1. 来自本地项目
     * 2. 或者有多个子节点
     * 3. 或者没有子节点
     */
    private fun findFirstProjectOrMultiChildNode(node: WidgetNode): WidgetNode {
        if (node.createdByLocalProject == true) return node
        val children = node.children
        if (children == null || children.size != 1) return node
        // 只有一个子节点且不是本地项目的，继续向下寻找
        return findFirstProjectOrMultiChildNode(children[0])
    }

    private fun org.jetbrains.jewel.foundation.lazy.tree.ChildrenGeneratorScope<WidgetNode>.appendChildNodeWithSimplification(node: WidgetNode) {
        val key = node.valueId ?: "node_${System.identityHashCode(node)}"
        
        // 如果是紧凑模式，检查是否需要“跳过”当前节点
        if (isCompactMode.value && node.createdByLocalProject != true) {
            val children = node.children
            if (children != null && children.size == 1) {
                // 跳过当前节点，递归处理子节点
                appendChildNodeWithSimplification(children[0])
                return
            }
        }

        if (node.children?.isNotEmpty() == true) {
            addNode(node, id = key) {
                node.children.forEach { child ->
                    appendChildNodeWithSimplification(child)
                }
            }
        } else {
            addLeaf(node, id = key)
        }
    }
    
    fun selectNode(node: WidgetNode) {
        _selectedNode.value = node
        // 加载节点的详细属性
        loadNodeProperties(node)
    }
    
    /**
     * 加载选中节点的详细属性
     */
    private fun loadNodeProperties(node: WidgetNode) {
        val valueId = node.valueId ?: return
        val isolateId = currentIsolateId ?: return
        
        coroutineScope.launch(Dispatchers.IO) {
            _isLoadingProperties.value = true
            try {
                val response = vmService.getProperties(
                    isolateId = isolateId,
                    groupName = objectGroup,
                    diagnosticsNodeId = valueId
                )
                _selectedNodeProperties.value = response.result
            } catch (e: Exception) {
                logger.warn("Failed to load properties for node: ${node.description}", e)
                _selectedNodeProperties.value = emptyList()
            } finally {
                _isLoadingProperties.value = false
            }
        }
    }

    fun dispose() {
        coroutineScope.launch(Dispatchers.IO) {
            currentIsolateId?.let { isolateId ->
                try {
                    vmService.disposeGroup(isolateId, objectGroup)
                } catch (e: Exception) {
                    logger.warn("Error disposing widget tree group", e)
                }
            }
        }
    }
    
    /**
     * 执行搜索
     */
    fun performSearch(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchMatches.value = emptySet()
            _matchedNodes.value = emptyList()
            _currentMatchIndex.value = 0
            return
        }

        val tree = _treeState.value ?: return
        val matches = mutableSetOf<String>()
        val orderedMatches = mutableListOf<WidgetNode>()
        
        // 递归搜索当前树中的节点
        tree.roots.forEach { root ->
            searchNodeRecursive(root.data, query.lowercase(), matches, orderedMatches)
        }
        
        _searchMatches.value = matches
        _matchedNodes.value = orderedMatches
        _currentMatchIndex.value = if (orderedMatches.isNotEmpty()) 1 else 0
    }

    /**
     * 导航到下一个搜索结果
     */
    fun searchNext() {
        val matches = _matchedNodes.value
        if (matches.isEmpty()) return
        var next = _currentMatchIndex.value + 1
        if (next > matches.size) next = 1
        _currentMatchIndex.value = next
    }

    /**
     * 导航到上一个搜索结果
     */
    fun searchPrevious() {
        val matches = _matchedNodes.value
        if (matches.isEmpty()) return
        var prev = _currentMatchIndex.value - 1
        if (prev < 1) prev = matches.size
        _currentMatchIndex.value = prev
    }

    /**
     * 选择特定的搜索结果
     */
    fun selectSearchResult(nodeId: String) {
        val matches = _matchedNodes.value
        val index = matches.indexOfFirst { (it.valueId ?: "node_${System.identityHashCode(it)}") == nodeId }
        if (index != -1) {
            _currentMatchIndex.value = index + 1
        }
    }
    
    private fun searchNodeRecursive(
        node: WidgetNode, 
        query: String, 
        matches: MutableSet<String>,
        orderedMatches: MutableList<WidgetNode>
    ) {
        val type = node.widgetRuntimeType?.lowercase() ?: ""
        val description = node.description?.lowercase() ?: ""
        val textPreview = node.textPreview?.lowercase() ?: ""
        val key = node.valueId ?: "node_${System.identityHashCode(node)}"
        
        // 检查当前节点是否匹配 (包括 Widget 类型、描述和文本内容)
        if (type.contains(query) || description.contains(query) || textPreview.contains(query)) {
            matches.add(key)
            orderedMatches.add(node)
        }
        
        // 检查子节点
        node.children?.forEach { child ->
            searchNodeRecursive(child, query, matches, orderedMatches)
            
            // 如果子节点匹配，父节点也应该标记为匹配（为了展开路径，虽然这里我们只在 UI 高亮匹配项）
            // 注意：如果只要高亮匹配项，这里不仅需要添加匹配项，还需要一种机制来自动展开父节点
            // 简单起见，我们先只收集直接匹配的节点 ID
        }
    }
    
    /**
     * 跳转到代码
     * 先尝试直接跳转（如果有位置信息），如果没有则请求详细信息再跳转
     */
    fun jumpToCode(node: WidgetNode) {
        val location = node.creationLocation
        if (location != null) {
            navigateToIDE(location)
            return
        }
        
        // 如果没有位置信息，且有 valueId，则尝试获取详细信息
        val valueId = node.valueId ?: return
        val isolateId = currentIsolateId ?: return
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 加载详细信息
                val detailedNode = vmService.getWidgetNodeDetails(isolateId, objectGroup, valueId)
                val detailLocation = detailedNode?.creationLocation
                if (detailLocation != null) {
                    navigateToIDE(detailLocation)
                } else {
                     logger.info("未找到节点 ${node.widgetRuntimeType} 的 creationLocation")
                }
            } catch (e: Exception) {
                logger.error("Error jumping to code", e)
            }
        }
    }
    
    // 执行 IDE 跳转
    private fun navigateToIDE(location: vm.element.CreationLocation) {
        val fileUri = location.file
        val line = location.line - 1
        val column = location.column - 1
        
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                // 处理 fileUri，如果已经是 file:// 开头则不再拼接
                val url = if (fileUri.startsWith("file:")) fileUri else "file://$fileUri"
                val virtualFile = com.intellij.openapi.vfs.VirtualFileManager.getInstance().findFileByUrl(url)
                
                if (virtualFile != null) {
                    com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
                    if (editor != null && editor.virtualFile == virtualFile) {
                        val logicalPosition = com.intellij.openapi.editor.LogicalPosition(line, column)
                        editor.caretModel.moveToLogicalPosition(logicalPosition)
                        editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                    }
                } else {
                     logger.warn("Failed to find virtual file for url: $url")
                }
            } catch (e: Exception) {
                logger.error("Navigation failed", e)
            }
        }
    }
}

