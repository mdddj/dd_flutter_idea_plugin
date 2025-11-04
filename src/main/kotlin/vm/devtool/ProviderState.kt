package vm.devtool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.SplitLayoutState
import vm.VmService
import vm.logging.Logging

/**
 * Provider 组件的状态管理类
 * 用于管理 Provider 列表、选中项、分割面板状态等
 */
class ProviderState(
    private val vmService: VmService
) {
    // 使用 vmService 的 scope，这样不会因为 Compose 重组而被取消
    private val scope: CoroutineScope get() = vmService.coroutineScope
    // Provider 列表
    var providers by mutableStateOf<List<ProviderNode>>(emptyList())
        private set

    // 当前选中的 Provider
    var selectedProvider by mutableStateOf<ProviderNode?>(null)

    // 分割面板状态
    var splitState by mutableStateOf(SplitLayoutState(0.3f))

    // 是否正在加载
    var isLoading by mutableStateOf(false)
        private set

    // 错误信息
    var error by mutableStateOf<String?>(null)
        private set

    /**
     * 刷新 Provider 列表
     */
    fun refreshProviders() {
        scope.launch {
            isLoading = true
            error = null
            try {
                providers = ProviderHelper.getProviderNodes(vmService)
                Logging.getLogger().logInformation("Loaded ${providers.size} providers")
            } catch (e: Exception) {
                // 检查是否是控制流异常
                if (isControlFlowException(e)) {
                    // 取消异常，静默处理
                    return@launch
                }
                error = e.message ?: "Failed to load providers"
                Logging.getLogger().logError("Failed to load providers", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * 选择一个 Provider
     */
    fun selectProvider(provider: ProviderNode) {
        selectedProvider = provider
    }

    /**
     * 清除选中
     */
    fun clearSelection() {
        selectedProvider = null
    }
}

/**
 * 实例节点查看器的状态管理类
 */
class InstanceNodeState(
    val vmService: VmService,
    val path: InstancePath,
    val parent: InstanceDetails? = null,
    val parentInstanceId: String? = null,
    val field: ObjectField? = null
) {
    // 实例详情
    var details by mutableStateOf<InstanceDetails?>(null)

    // 错误信息
    var error by mutableStateOf<String?>(null)

    // 是否展开
    var isExpanded by mutableStateOf(path.pathToProperty.isEmpty())

    // 刷新触发器
    var refreshTrigger by mutableStateOf(0)
        private set

    /**
     * 触发刷新
     */
    fun refresh() {
        refreshTrigger++
    }

    /**
     * 切换展开状态
     */
    fun toggleExpand() {
        isExpanded = !isExpanded
    }

    /**
     * 加载实例详情
     */
    suspend fun loadDetails() {
        try {
            details = ProviderHelper.getInstanceDetails(vmService, path, parent)
            error = null
        } catch (e: Exception) {
            // 检查是否是控制流异常
            if (isControlFlowException(e)) {
                // 取消异常，静默处理
                throw e
            }
            error = e.message ?: "An unknown error occurred"
            details = null
        }
    }
}
