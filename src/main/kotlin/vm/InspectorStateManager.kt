package vm

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import vm.element.Event
import vm.element.EventKind.Extension
import vm.element.NavigatorLocationInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Flutter Inspector状态管理器
 * 负责监听和管理Inspector Overlay的状态变化
 */
class InspectorStateManager(
    private val vmService: VmService,
) : Disposable, VmService.VmEventListener {

     val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = CopyOnWriteArrayList<InspectorStateListener>()


    // 当前状态
    @Volatile
    private var currentOverlayState = false
    private val _overlayState = MutableStateFlow(false)
    val overlayState: StateFlow<Boolean> = _overlayState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigatorLocationInfo>()
    val navigationEvents: SharedFlow<NavigatorLocationInfo> = _navigationEvents.asSharedFlow()

    // 状态监听器接口
    interface InspectorStateListener {
        fun onOverlayStateChanged(enabled: Boolean)
        fun navigate(result: NavigatorLocationInfo)
    }

    init {
        listenExtensionData()
    }


    private fun listenExtensionData() {
        println("开始监听 vm event 事件。")
        vmService.addEventListener(this)

    }


    /**
     * 通知所有监听器状态变化
     */
    private fun notifyStateChanged(enabled: Boolean) {
        listeners.forEach { listener ->
            try {
                listener.onOverlayStateChanged(enabled)
            } catch (e: Exception) {
                println("通知Inspector状态变化失败: ${e.message}")
            }
        }
    }

    /**
     * 添加状态监听器
     */
    fun addStateListener(listener: InspectorStateListener) {
        listeners.add(listener)
        listener.onOverlayStateChanged(currentOverlayState)
    }

    /**
     * 移除状态监听器
     */
    fun removeStateListener(listener: InspectorStateListener) {
        listeners.remove(listener)
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): Boolean = currentOverlayState

    /**
     * 手动更新状态（当我们主动改变状态时调用）
     */
    fun updateState(enabled: Boolean) {
        if (currentOverlayState != enabled) {
            currentOverlayState = enabled
            _overlayState.value = enabled
            notifyStateChanged(enabled)
        }
    }

    override fun dispose() {
        scope.cancel()
        listeners.clear()
        vmService.removeEventListener(this)
    }

    override fun onVmEvent(streamId: String, event: Event) {
        val kind = event.getKind()
        val exKind = event.getExtensionKind()
        when (kind) {
            Extension -> {
                when (exKind) {
                    "Flutter.ServiceExtensionStateChanged" -> {
                        val data = event.getExtensionData()
                        if (data != null) {
                            val extName = data.json.get("extension")
                            if (extName.asString == "ext.flutter.inspector.show") {
                                val value = data.json.get("value").asBoolean
                                currentOverlayState = value
                                _overlayState.value = value
                                notifyStateChanged(value)
                            }
                        }
                    }

                    "navigate" -> {
                        val data = event.getExtensionData()
                        if (data != null) {
                            val data = Gson().fromJson(data.json, NavigatorLocationInfo::class.java)
                            scope.launch {
                                _navigationEvents.emit(data)
                            }
                            listeners.forEach {
                                it.navigate(data)
                            }
                        }
                    }

                    else -> {}
                }

            }

            else -> {}
        }
    }


    companion object {
        private val stateManagers = ConcurrentHashMap<String, InspectorStateManager>()

        /**
         * 获取或创建状态管理器
         */
        fun getOrCreate(vmService: VmService, isolateId: String): InspectorStateManager {
            val key = "${vmService.hashCode()}_$isolateId"
            return stateManagers.computeIfAbsent(key) {
                InspectorStateManager(vmService)
            }
        }

        /**
         * 清理状态管理器
         */
        fun cleanup(vmService: VmService, isolateId: String) {
            val key = "${vmService.hashCode()}_$isolateId"
            stateManagers.remove(key)?.dispose()
        }
    }
}