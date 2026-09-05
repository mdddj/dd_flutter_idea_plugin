package vm.devtool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import vm.VmService
import vm.VmService.VmEventListener
import vm.element.Event
import vm.element.EventKind
import vm.logging.Logging

/**
 * Riverpod DevTool 的状态持有者。
 *
 * 对齐官方 riverpod_devtool 的 framesProvider / filteredProvidersProvider：
 * - 维护帧列表与每帧累积的 ElementState
 * - 增量刷新（收到 `riverpod:new_event` 扩展事件时只拉取新帧）
 * - 检测热重启并重置数据
 */
class RiverpodState(private val vmService: VmService) {

    private val scope: CoroutineScope get() = vmService.coroutineScope
    private var riverpodEval: EvalOnDartLibrary? = null
    private var lastFrameCount = 0

    /** 累积的元素状态（增量刷新用） */
    private var accumulatedElements = mutableMapOf<String, RiverpodElementState>()

    var isAvailable by mutableStateOf<Boolean?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var frames by mutableStateOf<List<RiverpodFrame>>(emptyList())
        private set

    /** 每帧的 state 变更：elementId -> statePath（仅含该帧内的新增/更新） */
    var elementChangesPerFrame by mutableStateOf<List<Map<String, String>>>(emptyList())
        private set

    /** 每帧的累积快照：elementId -> ElementState */
    var elementsPerFrame by mutableStateOf<List<Map<String, RiverpodElementState>>>(emptyList())
        private set

    var providers by mutableStateOf<List<RiverpodProviderInfo>>(emptyList())
        private set

    var selectedProvider by mutableStateOf<RiverpodProviderInfo?>(null)

    var selectedFrameIndex by mutableIntStateOf(0)

    val maxFrameIndex: Int get() = frames.maxOfOrNull { it.index } ?: 0

    private var refreshJob: Job? = null
    private var hotRestartJob: Job? = null

    private val extensionListener = object : VmEventListener {
        override fun onVmEvent(streamId: String, event: Event) {
            if (streamId != VmService.EXTENSION_STREAM_ID) return
            if (event.getExtensionKind() != "riverpod:new_event") return
            val extensionData = event.json.get("extensionData")?.asJsonObject ?: return
            val offset = extensionData.get("offset")?.asInt ?: return
            if (offset >= lastFrameCount) {
                scope.launch { refresh() }
            }
        }
    }

    init {
        // 监听热重启：IsolateExit 后 RiverpodDevtool.instance 会被重置，
        // 需要清空本地数据并在重启完成后重新初始化。
        hotRestartJob = scope.launch {
            vmService.vmEvents
                .catch { }
                .collect { kind ->
                    when (kind) {
                        EventKind.IsolateExit -> {
                            resetData()
                            scope.launch { reinit() }
                        }
                        else -> Unit
                    }
                }
        }
    }

    fun init() {
        vmService.addEventListener(extensionListener)
        scope.launch { reinit() }
    }

    private suspend fun reinit() {
        isLoading = true
        error = null
        try {
            val eval = RiverpodHelper.getRiverpodEval(vmService)
            if (eval == null) {
                isAvailable = false
                error = "Riverpod package not loaded in app"
                return
            }
            riverpodEval = eval
            val available = RiverpodHelper.checkAvailable(eval, vmService)
            isAvailable = available
            if (!available) {
                error = "RiverpodDevtool not available. Make sure the app is running in debug mode."
                return
            }
            refresh()
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                isAvailable = false
                error = e.message ?: "Failed to initialize"
                Logging.getLogger().logError("Riverpod init error", e)
            }
        } finally {
            isLoading = false
        }
    }

    /**
     * 增量刷新：只拉取新帧。
     */
    fun refresh() {
        val eval = riverpodEval ?: return
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            isLoading = true
            error = null
            try {
                val startIndex = frames.size
                val newFrames = RiverpodHelper.getNewFrames(eval, vmService, startIndex)
                if (newFrames.isNotEmpty()) {
                    val (changes, snapshots) = RiverpodHelper.accumulateElements(newFrames, accumulatedElements)
                    accumulatedElements = snapshots.lastOrNull()?.toMutableMap() ?: accumulatedElements
                    elementChangesPerFrame = elementChangesPerFrame + changes
                    elementsPerFrame = elementsPerFrame + snapshots
                    frames = frames + newFrames
                    providers = RiverpodHelper.extractProviders(frames)
                }
                lastFrameCount = frames.size

                if (frames.isNotEmpty()) {
                    val latest = frames.last().index
                    // 默认跟随最新帧
                    if (selectedFrameIndex == 0 || selectedFrameIndex >= latest) {
                        selectedFrameIndex = latest
                    }
                }
                // 若选中的 provider 已不存在则清空选择
                if (selectedProvider != null &&
                    providers.none { it.elementId == selectedProvider!!.elementId }
                ) {
                    selectedProvider = null
                }
            } catch (e: Exception) {
                if (!isControlFlowException(e)) {
                    error = e.message ?: "Failed to load data"
                    Logging.getLogger().logError("Riverpod refresh error", e)
                }
            } finally {
                isLoading = false
            }
        }
    }

    /** 热重启时重置所有本地数据 */
    private fun resetData() {
        accumulatedElements.clear()
        frames = emptyList()
        elementChangesPerFrame = emptyList()
        elementsPerFrame = emptyList()
        providers = emptyList()
        lastFrameCount = 0
        selectedProvider = null
        selectedFrameIndex = 0
        isAvailable = null
    }

    fun selectProvider(provider: RiverpodProviderInfo) {
        selectedProvider = provider
        // 选中 provider 后跳转到它最后一次变更的帧
        if (provider.frameIndex in 0..maxFrameIndex) {
            selectedFrameIndex = provider.frameIndex
        }
    }

    fun selectFrame(index: Int) {
        if (index in 0..maxFrameIndex) {
            selectedFrameIndex = index
        }
    }

    fun navigateFrame(delta: Int) {
        val newIndex = selectedFrameIndex + delta
        if (newIndex in 0..maxFrameIndex) {
            selectedFrameIndex = newIndex
        }
    }

    fun getSelectedFrame(): RiverpodFrame? {
        return frames.find { it.index == selectedFrameIndex }
    }

    /** 获取某帧某 element 的累积状态 */
    fun getElementStateAt(elementId: String, frameIndex: Int): RiverpodElementState? {
        if (frameIndex !in elementsPerFrame.indices) return null
        return elementsPerFrame[frameIndex][elementId]
    }

    /** 获取某 element 在当前选中帧的状态路径 */
    fun getCurrentStatePath(elementId: String): String? {
        return getElementStateAt(elementId, selectedFrameIndex)?.statePath
    }

    /**
     * 计算用于 diff 的两个状态路径：
     * - current：选中帧（含）之前该 element 最后一次更新的 state
     * - previous：current 之前的最后一次更新的 state
     *
     * 若没有 previous（首次创建）或没有 state，返回 null。
     */
    fun getStateDiffPaths(elementId: String, frameIndex: Int): Pair<String, String>? {
        // 找到选中帧之前（含）最后一次更新
        var currentFrame = -1
        var currentPath: String? = null
        for (i in frameIndex downTo 0) {
            val path = elementChangesPerFrame.getOrNull(i)?.get(elementId)
            if (path != null) {
                currentFrame = i
                currentPath = path
                break
            }
        }
        if (currentFrame < 0 || currentPath.isNullOrBlank()) return null

        // 找到 current 之前的最后一次更新
        var previousPath: String? = null
        for (i in currentFrame - 1 downTo 0) {
            val path = elementChangesPerFrame.getOrNull(i)?.get(elementId)
            if (path != null) {
                previousPath = path
                break
            }
        }
        if (previousPath.isNullOrBlank()) return null

        return previousPath to currentPath
    }

    /** 某 element 在指定帧的状态（added/modified/disposed/unchanged），取该帧内最后一次事件的状态 */
    fun getStatusAt(elementId: String, frameIndex: Int): RiverpodProviderStatus? {
        val frame = frames.getOrNull(frameIndex) ?: return null
        var status: RiverpodProviderStatus? = null
        for (event in frame.events) {
            val meta = event.providerMeta ?: continue
            if (meta.elementId != elementId) continue
            status = when (event.type) {
                RiverpodEventType.ProviderElementAdd -> RiverpodProviderStatus.Added
                RiverpodEventType.ProviderElementUpdate -> RiverpodProviderStatus.Modified
                RiverpodEventType.ProviderElementDispose -> RiverpodProviderStatus.Disposed
                else -> status
            }
        }
        return status
    }

    fun dispose() {
        vmService.removeEventListener(extensionListener)
        hotRestartJob?.cancel()
        refreshJob?.cancel()
    }
}
