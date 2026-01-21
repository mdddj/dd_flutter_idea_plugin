package shop.itbug.flutterx.widget_tree

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.foundation.lazy.tree.TreeBuilder
import org.jetbrains.jewel.foundation.lazy.tree.buildTree
import vm.VmService
import vm.element.WidgetNode
import vm.getDetailedWidgetTree
import vm.element.WidgetTreeResponse

class WidgetTreeViewModel(
    private val vmService: VmService,
    private val coroutineScope: CoroutineScope
) {
    private val logger = thisLogger()
    
    // 状态流
    private val _treeState = MutableStateFlow<Tree<WidgetNode>?>(null)
    val treeState = _treeState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // 选中的节点
    private val _selectedNode = MutableStateFlow<WidgetNode?>(null)
    val selectedNode = _selectedNode.asStateFlow()

    init {
        loadTree()
    }

    fun loadTree() {
        coroutineScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                val isolateId = vmService.getMainIsolateId()
                if (isolateId.isEmpty()) {
                    _error.value = "No active isolate found. Please ensure the app is running."
                    return@launch
                }
                
                // 获取 Widget Tree
                // 使用 debug 组
                val groupName = "debug" 
                val response = vmService.getDetailedWidgetTree(isolateId, groupName)
                
                if (response?.result != null) {
                    val jewelTree = buildWidgetTree(response.result)
                    _treeState.value = jewelTree
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

    private fun buildWidgetTree(root: WidgetNode): Tree<WidgetNode> = buildTree {
        appendWidgetNode(root)
    }

    private fun TreeBuilder.appendWidgetNode(node: WidgetNode) {
        // 如果有子节点，使用 addNode
        if (!node.children.isNullOrEmpty()) {
            addNode(node) {
                node.children?.forEach { child ->
                    appendWidgetNode(child)
                }
            }
        } else {
            // 如果没有子节点，或者是叶子节点
            // 注意：如果 node.hasChildren 为 true 但 children 为空，说明是懒加载或者被截断
            // 这里我们暂时作为叶子节点处理，后续可以添加懒加载逻辑
            addLeaf(node)
        }
    }
    
    fun selectNode(node: WidgetNode) {
        _selectedNode.value = node
        // 这里可以添加逻辑去 Flutter 端高亮选中节点
        // vmService.setSelectedWidget(...)
    }
}
