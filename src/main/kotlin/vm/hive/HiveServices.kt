package vm.hive

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import shop.itbug.flutterx.i18n.PluginBundle
import vm.VmService
import vm.devtool.LibraryNotFoundException
import vm.consumer.ServiceExtensionConsumer
import vm.element.Event
import vm.element.EventKind
import vm.element.RPCError
import vm.getVm
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HiveServices(
    project: Project,
    private val vmService: VmService,
) : CoroutineScope, VmService.VmEventListener, VmService.VmHotResetListener, Disposable {
    private val projectRoot = project.basePath
    private val runtimeResolver = HiveRuntimeValueResolver(vmService)

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val _state = MutableStateFlow(
        HiveInspectorState(
            status = HiveExtensionStatus.Loading,
            statusMessage = PluginBundle.get("vm.hive.status.idle"),
        ),
    )
    val state: StateFlow<HiveInspectorState> = _state.asStateFlow()

    private var initialized = false
    private var schemaRegistry = HiveSchemaRegistry(emptyMap(), emptyList())

    init {
        vmService.addEventListener(this)
        vmService.addEventHotResetListener(this)
        launch {
            schemaRegistry = HiveSchemaLoader.load(project.basePath)
            updateState {
                it.copy(schemaFiles = schemaRegistry.sourceFiles.size)
            }
        }
    }

    suspend fun initialize() {
        if (initialized) return
        initialized = true

        updateState {
            it.copy(
                status = HiveExtensionStatus.Loading,
                statusMessage = PluginBundle.get("vm.hive.status.loading"),
                refreshing = true,
            )
        }

        schemaRegistry = HiveSchemaLoader.load(projectRoot)
        updateState {
            it.copy(schemaFiles = schemaRegistry.sourceFiles.size)
        }
        refreshBoxes()
    }

    suspend fun refreshBoxes() {
        updateState {
            it.copy(
                refreshing = true,
                errorMessage = null,
                status = if (it.boxes.isEmpty()) HiveExtensionStatus.Loading else it.status,
                statusMessage = PluginBundle.get("vm.hive.status.refreshing"),
            )
        }

        try {
            val boxNames = listBoxes()
            updateState { current ->
                val nextBoxes = linkedMapOf<String, HiveBoxState>()
                boxNames.forEach { name ->
                    val existing = current.boxes[name]
                    nextBoxes[name] = existing?.copy(open = true) ?: HiveBoxState(name = name)
                }
                current.boxes.values
                    .filter { it.name !in nextBoxes.keys && !it.open }
                    .forEach { nextBoxes[it.name] = it }

                current.copy(
                    status = HiveExtensionStatus.Ready,
                    boxes = nextBoxes,
                    refreshing = false,
                    errorMessage = null,
                    statusMessage = PluginBundle.get("vm.hive.status.ready", nextBoxes.values.count { it.open }),
                )
            }
        } catch (error: Throwable) {
            handleRefreshError(error)
        }
    }

    suspend fun loadBox(boxName: String) {
        val box = state.value.boxes[boxName] ?: return
        if (box.loaded) return

        updateState {
            it.copy(statusMessage = PluginBundle.get("vm.hive.status.loading.box", boxName))
        }

        try {
            val frames = getBoxFrames(boxName)
            updateBox(boxName) {
                HiveBoxState(
                    name = boxName,
                    frames = LinkedHashMap(frames.associateBy { frame -> frame.key }),
                    open = true,
                    loaded = true,
                )
            }
            updateState {
                it.copy(
                    status = HiveExtensionStatus.Ready,
                    errorMessage = null,
                    statusMessage = PluginBundle.get("vm.hive.status.loaded.box", boxName, frames.size),
                )
            }
        } catch (error: Throwable) {
            handleNonFatalError(error)
        }
    }

    suspend fun loadLazyValue(boxName: String, key: Any) {
        updateState {
            it.copy(statusMessage = PluginBundle.get("vm.hive.status.loading.value", key.toString()))
        }

        try {
            val args = JsonObject().apply {
                add("name", keyToJson(boxName))
                add("key", keyToJson(key))
            }
            val response = callExtension(HiveConnectAction.LoadValue, args)
            val decodedValue = response?.let(::decodeWireValue)

            updateBox(boxName) { current ->
                val currentBox = current ?: HiveBoxState(name = boxName)
                val frames = LinkedHashMap(currentBox.frames)
                val previous = frames[key]
                if (previous != null) {
                    frames[key] = previous.copy(value = decodedValue, lazy = false)
                }
                currentBox.copy(
                    frames = frames,
                    loaded = true,
                )
            }
            updateState {
                it.copy(
                    status = HiveExtensionStatus.Ready,
                    errorMessage = null,
                    statusMessage = PluginBundle.get("vm.hive.status.value.ready", key.toString()),
                )
            }
        } catch (error: Throwable) {
            handleNonFatalError(error)
        }
    }

    suspend fun resolveRuntimeValue(boxName: String, key: Any) {
        updateState {
            it.copy(
                errorMessage = null,
                statusMessage = PluginBundle.get("vm.hive.status.resolving.fields", key.toString()),
            )
        }

        try {
            val isolateId = getMainIsolateId()
            val resolved = runtimeResolver.resolveBoxValue(isolateId, boxName, key)
            if (resolved == null) {
                updateState {
                    it.copy(
                        status = HiveExtensionStatus.Ready,
                        statusMessage = PluginBundle.get("vm.hive.status.resolve.fields.miss", key.toString()),
                    )
                }
                return
            }

            updateBox(boxName) { current ->
                val currentBox = current ?: HiveBoxState(name = boxName)
                val frames = LinkedHashMap(currentBox.frames)
                val previous = frames[key]
                if (previous != null) {
                    frames[key] = previous.copy(value = resolved, lazy = false)
                }
                currentBox.copy(
                    frames = frames,
                    loaded = true,
                )
            }
            updateState {
                it.copy(
                    status = HiveExtensionStatus.Ready,
                    errorMessage = null,
                    statusMessage = PluginBundle.get("vm.hive.status.resolved.fields", key.toString()),
                )
            }
        } catch (error: Throwable) {
            if (error is LibraryNotFoundException) {
                updateState {
                    it.copy(
                        status = HiveExtensionStatus.Ready,
                        errorMessage = PluginBundle.get("vm.hive.runtime.unavailable"),
                        statusMessage = PluginBundle.get("vm.hive.status.resolve.fields.miss", key.toString()),
                    )
                }
            } else {
                handleNonFatalError(error)
            }
        }
    }

    override fun onVmEvent(streamId: String, event: Event) {
        when (event.getKind()) {
            EventKind.Extension -> handleExtensionEvent(event)
            EventKind.ServiceExtensionAdded -> {
                if (event.getExtensionRPC()?.startsWith("ext.hive_ce.") == true) {
                    launch { refreshBoxes() }
                }
            }

            else -> Unit
        }
    }

    override fun onExit() {
        updateState {
            it.copy(
                status = HiveExtensionStatus.Loading,
                boxes = emptyMap(),
                refreshing = true,
                errorMessage = null,
                statusMessage = PluginBundle.get("vm.hive.status.restarting"),
            )
        }
    }

    override fun onStart() {
        launch {
            delay(500)
            schemaRegistry = HiveSchemaLoader.load(projectRoot)
            updateState {
                it.copy(schemaFiles = schemaRegistry.sourceFiles.size)
            }
            refreshBoxes()
        }
    }

    override fun dispose() {
        vmService.removeEventListener(this)
        vmService.removeEventHotResetListener(this)
        job.cancel()
    }

    private fun handleExtensionEvent(event: Event) {
        val data = event.getExtensionData()?.json ?: return
        when (event.getExtensionKind()) {
            HiveConnectEvent.BoxRegistered.event -> {
                launch {
                    val name = data.get("name")?.takeUnless { it is JsonNull }?.asString ?: return@launch
                    updateBox(name) { current -> current?.copy(open = true) ?: HiveBoxState(name = name) }
                    refreshBoxes()
                }
            }

            HiveConnectEvent.BoxUnregistered.event -> {
                val name = data.get("name")?.takeUnless { it is JsonNull }?.asString ?: return
                updateBox(name) { current -> current?.copy(open = false) ?: HiveBoxState(name = name, open = false) }
                updateState {
                    it.copy(statusMessage = PluginBundle.get("vm.hive.status.closed.box", name))
                }
            }

            HiveConnectEvent.BoxEvent.event -> {
                val boxName = data.get("box")?.asString ?: return
                val frameJson = data.getAsJsonObject("frame") ?: return
                val frame = parseFrame(frameJson)
                updateBox(boxName) { current ->
                    val box = current ?: HiveBoxState(name = boxName)
                    val frames = LinkedHashMap(box.frames)
                    if (frame.deleted) {
                        frames.remove(frame.key)
                    } else {
                        frames[frame.key] = frame
                    }
                    box.copy(
                        frames = frames,
                        open = true,
                    )
                }
                updateState {
                    it.copy(
                        status = HiveExtensionStatus.Ready,
                        statusMessage = PluginBundle.get("vm.hive.status.updated.box", boxName),
                    )
                }
            }
        }
    }

    private suspend fun listBoxes(): List<String> {
        val response = callExtension(HiveConnectAction.ListBoxes)
        val array = response as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            element.takeUnless { it is JsonNull }?.asString
        }
    }

    private suspend fun getBoxFrames(boxName: String): List<HiveInspectorFrame> {
        val args = JsonObject().apply {
            addProperty("name", boxName)
        }
        val response = callExtension(HiveConnectAction.GetBoxFrames, args)
        val array = response as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            element.takeIf { it.isJsonObject }?.asJsonObject?.let(::parseFrame)
        }
    }

    private suspend fun callExtension(action: HiveConnectAction, args: JsonObject? = null): JsonElement? {
        val isolateId = getMainIsolateId()
        return suspendCancellableCoroutine { continuation ->
            val params = JsonObject().apply {
                if (args != null) {
                    addProperty("args", args.toString())
                }
            }
            vmService.callServiceExtension(
                isolateId,
                action.method,
                params,
                object : ServiceExtensionConsumer {
                    override fun received(result: JsonObject) {
                        if (continuation.isActive) {
                            continuation.resume(result.get("result"))
                        }
                    }

                    override fun onError(error: RPCError) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(error.exception)
                        }
                    }
                },
            )
        }
    }

    private suspend fun getMainIsolateId(): String {
        val cached = vmService.getMainIsolateId()
        if (cached.isNotBlank()) return cached

        val vm = vmService.getVm()
        return vm.getIsolates().find { it.getName() == "main" }?.getId()
            ?: error("Main isolate not found")
    }

    private fun parseFrame(json: JsonObject): HiveInspectorFrame {
        val key = jsonToNative(json.get("key")) ?: "null"
        val lazy = json.get("lazy")?.asBoolean ?: false
        val deleted = json.get("deleted")?.asBoolean ?: false
        val valueElement = json.get("value")

        val value = if (lazy || deleted || valueElement == null || valueElement is JsonNull) {
            null
        } else {
            decodeWireValue(valueElement)
        }

        return HiveInspectorFrame(
            key = key,
            value = value,
            lazy = lazy,
            deleted = deleted,
        )
    }

    private fun decodeWireValue(element: JsonElement): Any? {
        val bytes = jsonArrayToByteArray(element as? JsonArray) ?: return jsonToNative(element)
        return runCatching {
            HiveRawObjectReader(schemaRegistry.types, bytes).read()
        }.getOrElse {
            bytes
        }
    }

    private fun jsonArrayToByteArray(array: JsonArray?): ByteArray? {
        array ?: return null
        if (array.any { !it.isJsonPrimitive || !it.asJsonPrimitive.isNumber }) {
            return null
        }

        return ByteArray(array.size()) { index ->
            array[index].asInt.toByte()
        }
    }

    private fun jsonToNative(element: JsonElement?): Any? {
        if (element == null || element is JsonNull) return null
        if (element.isJsonObject) {
            val obj = linkedMapOf<String, Any?>()
            element.asJsonObject.entrySet().forEach { (key, value) ->
                obj[key] = jsonToNative(value)
            }
            return obj
        }
        if (element.isJsonArray) {
            return element.asJsonArray.map { jsonToNative(it) }
        }

        val primitive = element.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isString -> primitive.asString
            primitive.isNumber -> {
                val asString = primitive.asString
                if (asString.contains('.') || asString.contains('e', true)) {
                    primitive.asDouble
                } else {
                    primitive.asLong
                }
            }

            else -> primitive.asString
        }
    }

    private fun keyToJson(value: Any): JsonElement = when (value) {
        is String -> com.google.gson.JsonPrimitive(value)
        is Number -> com.google.gson.JsonPrimitive(value)
        is Boolean -> com.google.gson.JsonPrimitive(value)
        else -> com.google.gson.JsonPrimitive(value.toString())
    }

    private fun handleRefreshError(error: Throwable) {
        if (error.isHiveExtensionUnavailable()) {
            updateState {
                it.copy(
                    status = HiveExtensionStatus.Unavailable,
                    boxes = emptyMap(),
                    refreshing = false,
                    errorMessage = PluginBundle.get("vm.hive.unavailable.message"),
                    statusMessage = PluginBundle.get("vm.hive.unavailable.status"),
                )
            }
            return
        }

        updateState {
            it.copy(
                status = HiveExtensionStatus.Ready,
                refreshing = false,
                errorMessage = error.message ?: PluginBundle.get("vm.hive.error.unknown"),
                statusMessage = PluginBundle.get("vm.hive.error.status"),
            )
        }
    }

    private fun handleNonFatalError(error: Throwable) {
        updateState {
            it.copy(
                status = if (it.status == HiveExtensionStatus.Unavailable) it.status else HiveExtensionStatus.Ready,
                errorMessage = error.message ?: PluginBundle.get("vm.hive.error.unknown"),
                statusMessage = PluginBundle.get("vm.hive.error.status"),
            )
        }
    }

    private fun updateBox(name: String, transform: (HiveBoxState?) -> HiveBoxState?) {
        updateState { current ->
            val nextBoxes = LinkedHashMap(current.boxes)
            val updated = transform(nextBoxes[name])
            if (updated == null) {
                nextBoxes.remove(name)
            } else {
                nextBoxes[name] = updated
            }
            current.copy(boxes = nextBoxes)
        }
    }

    private fun updateState(transform: (HiveInspectorState) -> HiveInspectorState) {
        _state.update(transform)
    }
}

private fun Throwable.isHiveExtensionUnavailable(): Boolean {
    val message = message ?: return false
    return message.contains("ext.hive_ce") &&
            (message.contains("Method not found", ignoreCase = true) ||
                    message.contains("not registered", ignoreCase = true) ||
                    message.contains("unknown rpc", ignoreCase = true))
}
