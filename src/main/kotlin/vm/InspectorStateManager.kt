package vm

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import vm.element.Event
import vm.element.EventKind.Extension
import vm.element.NavigatorLocationInfo

/**
 * Flutter Inspector状态管理器
 * 负责监听和管理Inspector Overlay的状态变化
 */
class InspectorStateManager(
    private val vmService: VmService,
) : Disposable, VmService.VmEventListener, VmService.VmHotResetListener {

    val scope get() =  vmService.coroutineScope
    private val logger = thisLogger()

    @Volatile
    private var currentOverlayState = false
    private val _overlayState = MutableStateFlow(false)
    val overlayState: StateFlow<Boolean> = _overlayState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigatorLocationInfo>()
    val navigationEvents: SharedFlow<NavigatorLocationInfo> = _navigationEvents.asSharedFlow()


    init {
        listenExtensionData()
    }


    private fun listenExtensionData() {
        println("开始监听 vm event 事件。")
        vmService.addEventListener(this)
        vmService.addEventHotResetListener(this)

    }


    override fun dispose() {
        // 在 service中处理
//        vmService.removeEventListener(this)
//        vmService.removeEventHotResetListener(this)
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
                        }
                    }

                    else -> {}
                }

            }

            else -> {}
        }
    }

    override fun onExit() {
        logger.info("dart vm 热重启on exit")
    }

    override fun onStart() {
        logger.info("dart vm 热重启on start")
        currentOverlayState = false
        _overlayState.value = false
    }


}



